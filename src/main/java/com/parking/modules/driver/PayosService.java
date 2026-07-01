package com.parking.modules.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Payment;
import com.parking.entity.Reservation;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tich hop PayOS goi thang REST API (khong dung SDK de tranh phu thuoc version).
 * - createDepositLink: tao link + QR cho tien coc dat cho.
 * - handleWebhook: PayOS goi ve khi thanh toan xong -> xac nhan coc.
 * Doc: https://payos.vn/docs/api/
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
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

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public PayosLinkResponse createDepositLink(Long reservationId, String username) {
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
                .transactionReference(String.valueOf(orderCode))
                .build());

        return resp;
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
        paymentRepository.findFirstByTransactionReference(orderCode).ifPresent(payment -> {
            if ("Success".equals(payment.getPaymentStatus())) {
                return; // da xu ly
            }
            payment.setPaymentStatus("Success");
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);

            Reservation r = payment.getReservation();
            if (r != null && "Pending".equals(r.getStatus())) {
                r.setDepositStatus("Paid");
                r.setStatus("Confirmed");
                reservationRepository.save(r);
            }
        });
    }

    @Transactional
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
            throw new BusinessRuleException("Loi goi PayOS de kiem tra giao dich: " + e.getMessage(), "PAYOS_ERROR");
        }
        if (root == null || !"00".equals(root.path("code").asText())) {
            String desc = root == null ? "khong co phan hoi" : root.path("desc").asText();
            throw new BusinessRuleException("Khong the lay thong tin tu PayOS: " + desc, "PAYOS_ERROR");
        }

        JsonNode data = root.path("data");
        String status = data.path("status").asText();

        if ("PAID".equals(status)) {
            paymentRepository.findFirstByTransactionReference(String.valueOf(orderCode)).ifPresent(payment -> {
                if ("Success".equals(payment.getPaymentStatus())) {
                    return;
                }
                payment.setPaymentStatus("Success");
                payment.setPaymentTime(LocalDateTime.now());
                paymentRepository.save(payment);

                Reservation r = payment.getReservation();
                if (r != null && "Pending".equals(r.getStatus())) {
                    r.setDepositStatus("Paid");
                    r.setStatus("Confirmed");
                    reservationRepository.save(r);
                }
            });
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
            if (sb.length() > 0) sb.append('&');
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
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Loi tinh HMAC", e);
        }
    }

    /**
     * Build a unique orderCode that stays within JS Number.MAX_SAFE_INTEGER (< 10^15).
     * Formula: (reservationId % 10^8) * 10^6 + (currentTimeMillis % 10^6)
     * Max value: 99_999_999 * 1_000_000 + 999_999 = 99_999_999_999_999 < 9_007_199_254_740_991.
     * Retries up to 5 times if a collision is found in the Payment table.
     */
    private long buildOrderCode(Long reservationId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            long code = (reservationId % 100_000_000L) * 1_000_000L
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
