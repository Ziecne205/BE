package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
import com.parking.entity.ParkingSession;
import com.parking.entity.Payment;
import com.parking.entity.User;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.PaymentRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentDriverService {

    private static final List<String> PAYABLE_STATUSES = List.of("Admitted", "Parked", "Moved");

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final PayosService payosService;
    private final PlatformTransactionManager txManager;

    public String createMockPaymentUrl(PaymentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ParkingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Ownership check
        if (session.getDriver() == null
                || !session.getDriver().getUserId().equals(user.getUserId())) {
            throw new BusinessRuleException("Ban khong co quyen thanh toan phien nay");
        }

        // Status guard
        if (!PAYABLE_STATUSES.contains(session.getStatus())) {
            throw new BusinessRuleException(
                    "Khong the thanh toan phien o trang thai: " + session.getStatus());
        }

        // Always calculate server-side fee; ignore client-supplied amount
        BigDecimal fee = pricingService.calculateFee(
                session.getVehicleType().getVehicleTypeId(),
                session.getEntryTime(),
                null);

        String mockTxnRef = "MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "https://mock-payment-gateway.com/pay?txnRef=" + mockTxnRef
                + "&amount=" + fee
                + "&sessionId=" + request.getSessionId();
    }

    public Payment processMockCallback(String txnRef, Long sessionId, String status, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Ownership check
        if (session.getDriver() == null
                || !session.getDriver().getUserId().equals(user.getUserId())) {
            throw new BusinessRuleException("Ban khong co quyen thanh toan phien nay");
        }

        // Status guard — only payable sessions can be completed
        if (!PAYABLE_STATUSES.contains(session.getStatus())) {
            throw new BusinessRuleException(
                    "Khong the thanh toan phien o trang thai: " + session.getStatus());
        }

        // Idempotency — reject if a successful payment already exists
        boolean alreadyPaid = paymentRepository.findBySession_SessionId(sessionId)
                .stream().anyMatch(p -> "Success".equals(p.getPaymentStatus()));
        if (alreadyPaid) {
            throw new BusinessRuleException("Phien nay da duoc thanh toan", "ALREADY_PAID");
        }

        BigDecimal fee = pricingService.calculateFee(
                session.getVehicleType().getVehicleTypeId(),
                session.getEntryTime(),
                LocalDateTime.now());

        Payment payment = Payment.builder()
                .session(session)
                .reservation(session.getReservation())
                .amount(fee)
                .paymentMethod("VNPay_Mock")
                .paymentTime(LocalDateTime.now())
                .paymentStatus("Success".equalsIgnoreCase(status) ? "Success" : "Failed")
                .paymentPurpose("Fee")
                .transactionReference(txnRef)
                .build();

        paymentRepository.save(payment);

        // Luu y: KHONG dong phien / giai phong slot o day. Xe co the da tra tien
        // online nhung van chua roi bai vat ly - phien chi thuc su Completed va
        // slot chi duoc giai phong khi bao ve quet the check-out tai cong
        // (SessionService.checkOut), de tranh 2 luong (thanh toan online va
        // check-out cong) giam sat state khong dong bo.
        return payment;
    }

    /**
     * Tao link/QR PayOS cho phi gui xe cua CHINH phien khach dang do (type=PARKING) — khach tu tra
     * online truoc khi ra. Giong luong staff fee-QR: chi tao QR + luu Payment "Pending" theo orderCode;
     * webhook doi chieu khi tra xong, con check-out that (mo barie) van do cong ra dam nhiem —
     * check-out se nhan ra phien da tra online nen khong thu phi 2 lan.
     *
     * Chay NGOAI transaction va chia 3 pha de khong giu ket noi DB trong luc goi PayOS (HTTP ~15s).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PayosLinkResponse createParkingLink(Long sessionId, String username) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        // Pha 1 (tx ngan): kiem tra quyen + trang thai + tinh phi (co truy cap lazy driver/vehicleType).
        ParkingFeeQuote quote = tx.execute(status -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            ParkingSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
            if (session.getDriver() == null
                    || !session.getDriver().getUserId().equals(user.getUserId())) {
                throw new BusinessRuleException("Ban khong co quyen thanh toan phien nay");
            }
            if (!PAYABLE_STATUSES.contains(session.getStatus())) {
                throw new BusinessRuleException(
                        "Khong the thanh toan phien o trang thai: " + session.getStatus());
            }
            boolean alreadyPaid = paymentRepository.findBySession_SessionId(sessionId)
                    .stream().anyMatch(p -> "Success".equals(p.getPaymentStatus()));
            if (alreadyPaid) {
                throw new BusinessRuleException("Phien nay da duoc thanh toan", "ALREADY_PAID");
            }
            BigDecimal fee = pricingService.calculateFee(
                    session.getVehicleType().getVehicleTypeId(), session.getEntryTime(), LocalDateTime.now());
            return new ParkingFeeQuote(session.getSessionId(), session.getLicensePlateIn(), fee);
        });

        // Pha 2 (khong giu tx): goi PayOS.
        PayosLinkResponse link = payosService.createLinkForAmount(
                quote.sessionId(), quote.fee().longValue(), "Phi gui xe " + quote.plate());

        // Pha 3 (tx ngan): luu Payment "Pending" theo orderCode de webhook/check-out doi chieu.
        tx.executeWithoutResult(status -> {
            ParkingSession s = sessionRepository.findById(quote.sessionId()).orElseThrow();
            paymentRepository.save(Payment.builder()
                    .session(s)
                    .reservation(s.getReservation())
                    .amount(BigDecimal.valueOf(link.getAmount()))
                    .paymentMethod("PayOS")
                    .paymentTime(LocalDateTime.now())
                    .paymentStatus("Pending")
                    .paymentPurpose("Fee")
                    .transactionReference(String.valueOf(link.getOrderCode()))
                    .build());
        });

        return link;
    }

    /** Gia tri nguyen thuy trich tu pha doc de dung sau khi transaction da dong (tranh lazy-init). */
    private record ParkingFeeQuote(Long sessionId, String plate, BigDecimal fee) {}
}
