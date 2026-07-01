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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class PaymentDriverService {

    private static final List<String> PAYABLE_STATUSES = List.of("Admitted", "Parked", "Moved");

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

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
                .amount(fee)
                .paymentMethod("VNPay_Mock")
                .paymentTime(LocalDateTime.now())
                .paymentStatus("Success".equalsIgnoreCase(status) ? "Success" : "Failed")
                .transactionReference(txnRef)
                .build();

        paymentRepository.save(payment);

        if ("Success".equalsIgnoreCase(status)) {
            session.setStatus("Completed");
            session.setExitTime(LocalDateTime.now());
            sessionRepository.save(session);
        }

        return payment;
    }
}
