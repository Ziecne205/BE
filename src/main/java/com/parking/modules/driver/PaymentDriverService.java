package com.parking.modules.driver;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentDriverService {

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public String createMockPaymentUrl(PaymentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ParkingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getDriver().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied");
        }

        if (!"Moved".equals(session.getStatus()) && !"Admitted".equals(session.getStatus())
                && !"Parked".equals(session.getStatus())) {
            throw new RuntimeException("Cannot pay for session in status: " + session.getStatus());
        }

        // Return a mock URL
        String mockTxnRef = "MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "https://mock-payment-gateway.com/pay?txnRef=" + mockTxnRef + "&amount=" + request.getAmount()
                + "&sessionId=" + request.getSessionId();
    }

    public Payment processMockCallback(String txnRef, Long sessionId, String status) {
        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        BigDecimal fee = pricingService.calculateFee(session.getVehicleType().getVehicleTypeId(),
                session.getEntryTime(), LocalDateTime.now());

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
