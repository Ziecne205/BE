package com.parking.modules.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Payment;
import com.parking.entity.Reservation;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Tich hop PayOS goi thang REST API (khong dung SDK de tranh phu thuoc
 * version).
 * - createDepositLink: tao link + QR cho tien coc dat cho.
 * - handleWebhook: PayOS goi ve khi thanh toan xong -> xac nhan coc.
 * Doc: https://payos.vn/docs/api/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayosService {

    private static final String CREATE_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    @Value("${payos.client-id:}")
    private String clientId;
    @Value("${payos.api-key:}")
    private String apiKey;
    @Value("${payos.checksum-key:}")
    private String checksumKey;
    @Value("${payos.return-url:http://localhost:3000/driver/payment/return}")
    private String returnUrl;
    @Value("${payos.cancel-url:http://localhost:3000/driver/payment/return?cancel=true}")
    private String cancelUrl;
    // PayOS's public REST API has no generic "refund a completed payment" endpoint — only a
    // separate Payout product (POST /v1/payouts) requiring a funded payout balance and recipient
    // bank details, neither collected anywhere in this codebase today. Default off (the realistic
    // state of this merchant account); attemptRefundPaidDeposit() falls back to ManualRequired
    // bookkeeping whenever this is false, per phase-6-independent-items.md 6.1's explicit fallback.
    @Value("${payos.payout.enabled:false}")
    private boolean payoutEnabled;

    /** Cat khoang trang/xuong dong thua khi copy-paste key vao bien moi truong — tranh loi
     *  "Illegal character(s) in message header value" khi dat key vao HTTP header (x-client-id...). */
    @jakarta.annotation.PostConstruct
    void normalizeConfig() {
        if (clientId != null) clientId = clientId.trim();
        if (apiKey != null) apiKey = apiKey.trim();
        if (checksumKey != null) checksumKey = checksumKey.trim();
        if (returnUrl != null) returnUrl = returnUrl.trim();
        if (cancelUrl != null) cancelUrl = cancelUrl.trim();
    }

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    // Timeout ket noi/doc bat buoc: khong de mot lan goi PayOS treo vo han giu ket noi DB
    // (cac phuong thuc goi PayOS deu chay trong @Transactional).
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(factory);
    }

    @Transactional
    public PayosLinkResponse createDepositLink(UUID reservationId, String username) {
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessRuleException("PayOS chua duoc cau hinh (thieu key)", "PAYOS_NOT_CONFIGURED");
        }
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking #" + reservationId));
        if (r.getUser() == null || !r.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Ban khong co quyen thanh toan booking nay");
        }
        if (!"Pending".equals(r.getStatus())) {
            throw new BusinessRuleException("Booking khong o trang thai cho thanh toan coc");
        }

        long amount = r.getDepositAmount() == null ? 0 : r.getDepositAmount().longValue();
        if (amount < 1000) {
            amount = 2000; // PayOS yeu cau so tien toi thieu
        }
        long orderCode = buildOrderCode(reservationId);
        String description = truncate("Coc " + r.getLicensePlate(), 25);

        PayosLinkResponse resp = callCreatePaymentLink(orderCode, amount, description);

        // Luu Payment Pending de webhook doi chieu theo orderCode.
        paymentRepository.save(Payment.builder()
                .reservation(r)
                .amount(BigDecimal.valueOf(amount))
                .paymentMethod("PayOS")
                .paymentTime(LocalDateTime.now())
                .paymentStatus("Pending")
                .paymentPurpose("Deposit")
                .transactionReference(String.valueOf(orderCode))
                .build());

        return resp;
    }

    /**
     * Tao link/QR PayOS cho MOT so tien bat ky (dung cho phi gui xe dong tai cong ra — staff).
     * Ban than ham chi goi PayOS; caller (SessionService.createFeeLink) chiu trach nhiem luu
     * Payment "Pending" theo orderCode de webhook/polling doi chieu khi khach thanh toan, va de
     * check-out nhan ra phien da tra online (tranh thu phi 2 lan). Tai khoan nhan (PayOS keys) khong doi.
     */
    public PayosLinkResponse createLinkForAmount(long refId, long amount, String description) {
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessRuleException("PayOS chua duoc cau hinh (thieu key)", "PAYOS_NOT_CONFIGURED");
        }
        long safeAmount = amount < 1000 ? 2000 : amount; // PayOS yeu cau so tien toi thieu
        long orderCode = buildOrderCode(refId);
        return callCreatePaymentLink(orderCode, safeAmount, truncate(description, 25));
    }

    /** Nhu tren nhung refId la UUID (vd reservationId) thay vi so — dung chung cach bam voi
     * createDepositLink de tranh trung orderCode giua cac loai giao dich khac nhau tren cung 1 booking. */
    public PayosLinkResponse createLinkForAmount(UUID refId, long amount, String description) {
        return createLinkForAmount(Math.abs((long) refId.hashCode()), amount, description);
    }

    /**
     * Huy mot payment-request PayOS con "Pending" (chua thanh toan) — dung khi booking bi huy
     * truoc khi khach kip tra coc, de link/QR cu khong con dung duoc nua (item 6, Phase 6.1).
     */
    public void cancelPaymentLink(String orderCode, String reason) {
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessRuleException("PayOS chua duoc cau hinh (thieu key)", "PAYOS_NOT_CONFIGURED");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cancellationReason", reason == null || reason.isBlank() ? "Booking cancelled" : reason);

        JsonNode root;
        try {
            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(
                    CREATE_URL + "/" + orderCode + "/cancel", new HttpEntity<>(body, headers), JsonNode.class);
            root = resp.getBody();
        } catch (Exception e) {
            log.error("[PayOS-cancel] orderCode={} loi goi API: {}", orderCode, e.getMessage());
            throw new BusinessRuleException("Loi goi PayOS de huy link thanh toan: " + e.getMessage(), "PAYOS_ERROR");
        }
        if (root == null || !"00".equals(root.path("code").asText())) {
            String desc = root == null ? "khong co phan hoi" : root.path("desc").asText();
            throw new BusinessRuleException("PayOS tu choi huy link: " + desc, "PAYOS_ERROR");
        }
        log.info("[PayOS-cancel] orderCode={} da huy thanh cong ({})", orderCode, reason);
    }

    /**
     * Hoan coc da thanh toan (depositStatus=="Paid") khi booking bi huy. PayOS khong co endpoint
     * hoan tien chung cho payment-request da hoan tat — chi co san pham Payout rieng
     * (POST /v1/payouts) can so du payout da nap va thong tin ngan hang nguoi nhan, ca hai deu
     * chua duoc thu thap/cau hinh trong repo nay. Khi payoutEnabled=false (mac dinh, dung thuc te
     * hien tai) chuyen thang sang bookkeeping: Payment.refundStatus="ManualRequired" de xep vao
     * hang cho xu ly thu cong (xem GET /api/manager/payments?refundStatus=ManualRequired), va
     * KHONG nem loi — cancelWithRefund() van phai hoan tat cap nhat trang thai booking dung han.
     */
    @Transactional
    public void attemptRefundPaidDeposit(Reservation reservation, String reason) {
        Payment deposit = paymentRepository
                .findByReservation_ReservationIdAndPaymentPurpose(reservation.getReservationId(), "Deposit")
                .stream()
                .filter(p -> "Success".equals(p.getPaymentStatus()))
                .max(java.util.Comparator.comparing(Payment::getPaymentId))
                .orElse(null);
        if (deposit == null) {
            log.warn("[PayOS-refund] Reservation {} depositStatus=Paid nhung khong tim thay Payment Deposit Success tuong ung",
                    reservation.getReservationId());
            return;
        }
        if (payoutEnabled) {
            // Payout product chua duoc tich hop (thieu so du/thong tin ngan hang) — khi bat co
            // duoc cau hinh that, day la noi goi POST /v1/payouts. Cho den luc do, coi nhu that bai
            // va roi xuong nhanh fallback ben duoi thay vi gia vo thanh cong.
            log.warn("[PayOS-refund] payoutEnabled=true nhung Payout API chua duoc trien khai — fallback ManualRequired");
        }
        deposit.setRefundStatus("ManualRequired");
        paymentRepository.save(deposit);
        log.warn("[PayOS-refund] Reservation {} can hoan coc thu cong (paymentId={}, amount={}, ly do: {})",
                reservation.getReservationId(), deposit.getPaymentId(), deposit.getAmount(), reason);
    }

    /** Payment(paymentPurpose="Deposit", paymentStatus="Pending") gan nhat cua mot reservation, de
     * lay orderCode goi cancelPaymentLink khi booking bi huy truoc khi kip thanh toan coc. */
    public java.util.Optional<String> findPendingDepositOrderCode(Reservation reservation) {
        return paymentRepository
                .findByReservation_ReservationIdAndPaymentPurpose(reservation.getReservationId(), "Deposit")
                .stream()
                .filter(p -> "Pending".equals(p.getPaymentStatus()) && p.getTransactionReference() != null)
                .max(java.util.Comparator.comparing(Payment::getPaymentId))
                .map(Payment::getTransactionReference);
    }

    private PayosLinkResponse callCreatePaymentLink(long orderCode, long amount, String description) {
        // Chu ky: cac field sap xep theo alphabet.
        String signData = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        String signature = hmacSha256(signData, checksumKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        JsonNode root;
        try {
            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(
                    CREATE_URL, new HttpEntity<>(body, headers), JsonNode.class);
            root = resp.getBody();
        } catch (Exception e) {
            throw new BusinessRuleException("Loi goi PayOS: " + e.getMessage(), "PAYOS_ERROR");
        }
        if (root == null || !"00".equals(root.path("code").asText())) {
            String desc = root == null ? "khong co phan hoi" : root.path("desc").asText();
            throw new BusinessRuleException("PayOS tu choi: " + desc, "PAYOS_ERROR");
        }

        JsonNode data = root.path("data");
        return PayosLinkResponse.builder()
                .checkoutUrl(data.path("checkoutUrl").asText())
                .qrCode(data.path("qrCode").asText())
                .orderCode(orderCode)
                .amount(amount)
                .build();
    }

    /** PayOS webhook -> xac thuc chu ky, xac nhan coc cho reservation tuong ung. */
    @Transactional
    public void handleWebhook(JsonNode body) {
        JsonNode data = body.path("data");
        if (data.isMissingNode()) {
            return; // ping/test cua PayOS
        }
        String receivedSig = body.path("signature").asText("");
        if (!receivedSig.equals(signOfData(data))) {
            throw new BusinessRuleException("Chu ky webhook khong hop le");
        }

        String orderCode = data.path("orderCode").asText();
        markPaymentPaid(orderCode);
    }

    /**
     * Danh dau giao dich da thanh toan + xac nhan coc cho reservation (idempotent: bo qua neu
     * da Success). Dung chung boi webhook (handleWebhook) va polling (verifyPaymentStatus).
     */
    private void markPaymentPaid(String orderCode) {
        paymentRepository.findFirstByTransactionReference(orderCode).ifPresentOrElse(payment -> {
            if ("Success".equals(payment.getPaymentStatus())) {
                return; // da xu ly
            }
            payment.setPaymentStatus("Success");
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);

            Reservation r = payment.getReservation();
            // "Expired" duoc chap nhan cung "Pending": scheduler co the da het han booking trong luc
            // khach van dang thanh toan tren PayOS (race dieu kien voi expireUnpaidReservations).
            // PayOS da xac nhan PAID that (goi tu day) nen "hoi sinh" booking thay vi bo qua no.
            if (r != null && ("Pending".equals(r.getStatus()) || "Expired".equals(r.getStatus()))) {
                r.setDepositStatus("Paid");
                r.setStatus("Confirmed");
                reservationRepository.save(r);
                log.info("[PayOS-verify] orderCode={} -> Payment=Success, reservation #{} = Confirmed",
                        orderCode, r.getReservationId());
            } else {
                log.warn("[PayOS-verify] orderCode={} Payment=Success nhung reservation khong o trang thai Pending/Expired (r={})",
                        orderCode, r == null ? "null" : r.getStatus());
            }
        }, () -> log.warn("[PayOS-verify] orderCode={} PAID nhung KHONG tim thay Payment trong DB", orderCode));
    }

    // REQUIRES_NEW: cac caller khac (vd SessionExpiryScheduler.expireUnpaidReservations) chay
    // nhieu ban ghi trong CUNG mot transaction lon; neu ham nay dung propagation mac dinh (REQUIRED)
    // va nem loi (vd PAYMENT_NOT_PAID hoan toan binh thuong, khong phai bug), Spring se danh dau
    // CA transaction ngoai la rollback-only tai thoi diem nem — bat exception o caller khong cuu
    // duoc, toan bo batch (ke ca cac ban ghi khac da xu ly xong hop le) se bi rollback ngam khi
    // commit (UnexpectedRollbackException). Tach rieng transaction de 1 giao dich loi/chua thanh
    // toan khong lam anh huong cac giao dich khac trong cung 1 lan chay scheduler.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verifyPaymentStatus(long orderCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        JsonNode root;
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    CREATE_URL + "/" + orderCode, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            root = resp.getBody();
        } catch (Exception e) {
            log.error("[PayOS-verify] orderCode={} loi goi API: {}", orderCode, e.getMessage());
            throw new BusinessRuleException("Loi goi PayOS de kiem tra giao dich: " + e.getMessage(), "PAYOS_ERROR");
        }
        // Log nguyen phan hoi PayOS de chan doan (grep "[PayOS-verify]"). Day la noi hay "im lang" fail.
        log.info("[PayOS-verify] orderCode={} phan hoi tu PayOS: {}", orderCode, root);
        if (root == null || !"00".equals(root.path("code").asText())) {
            String desc = root == null ? "khong co phan hoi" : root.path("desc").asText();
            throw new BusinessRuleException("Khong the lay thong tin tu PayOS: " + desc, "PAYOS_ERROR");
        }

        JsonNode data = root.path("data");
        String status = data.path("status").asText();
        log.info("[PayOS-verify] orderCode={} status={}", orderCode, status);

        if ("PAID".equals(status)) {
            markPaymentPaid(String.valueOf(orderCode));
        } else {
            throw new BusinessRuleException("Giao dich nay chua duoc thanh toan (trang thai PayOS: " + status + ")", "PAYMENT_NOT_PAID");
        }
    }

    /** Chu ky cua object data: HMAC_SHA256 cua "k=v&..." voi key sap xep alphabet. */
    private String signOfData(JsonNode data) {
        TreeMap<String, String> sorted = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = data.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            sorted.put(e.getKey(), v == null || v.isNull() ? "" : v.asText());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0)
                sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return hmacSha256(sb.toString(), checksumKey);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Loi tinh HMAC", e);
        }
    }

    /**
     * Build a unique orderCode that stays within JS Number.MAX_SAFE_INTEGER (< 10^15).
     * reservationId la UUID (khong con la so) nen dung hashCode cua no thay cho modulo truc tiep.
     */
    private long buildOrderCode(UUID reservationId) {
        return buildOrderCode(Math.abs((long) reservationId.hashCode()));
    }

    /**
     * Build a unique orderCode that stays within JS Number.MAX_SAFE_INTEGER (< 10^15).
     * Formula: (idPart % 10^8) * 10^6 + (currentTimeMillis % 10^6)
     * Max value: 99_999_999 * 1_000_000 + 999_999 = 99_999_999_999_999 < 9_007_199_254_740_991.
     * Retries up to 5 times if a collision is found in the Payment table.
     */
    private long buildOrderCode(long idPart) {
        for (int attempt = 0; attempt < 5; attempt++) {
            long code = (idPart % 100_000_000L) * 1_000_000L
                    + (System.currentTimeMillis() % 1_000_000L);
            if (paymentRepository.findFirstByTransactionReference(String.valueOf(code)).isEmpty()) {
                return code;
            }
            // Brief pause to shift the ms suffix before retrying
            try { Thread.sleep(1); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        throw new BusinessRuleException("Khong the tao ma giao dich duy nhat, vui long thu lai", "ORDER_CODE_CONFLICT");
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
