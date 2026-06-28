package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
import com.parking.entity.ParkingSession;
import com.parking.entity.Payment;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class PaymentDriverService {

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PricingService pricingService;
    private final PayOS payOS;

    public String createMockPaymentUrl(PaymentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ParkingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getDriver().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied");
        }

        if (!"Moved".equals(session.getStatus()) && !"Admitted".equals(session.getStatus()) && !"Parked".equals(session.getStatus())) {
            throw new RuntimeException("Cannot pay for session in status: " + session.getStatus());
        }

        String mockTxnRef = "MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "https://mock-payment-gateway.com/pay?txnRef=" + mockTxnRef + "&amount=" + request.getAmount() + "&sessionId=" + request.getSessionId();
    }

    public Payment processMockCallback(String txnRef, Long sessionId, String status) {
        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        BigDecimal fee = pricingService.calculateFee(session.getVehicleType().getVehicleTypeId(), session.getEntryTime(), LocalDateTime.now());

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

    public PayosLinkResponse createPayosLink(PayosLinkRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long amount = 0;
        String description = "";
        Long orderCode = System.currentTimeMillis() / 1000L;
        Payment payment = Payment.builder()
                .paymentMethod("PayOS")
                .paymentTime(LocalDateTime.now())
                .paymentStatus("Pending")
                .transactionReference(String.valueOf(orderCode))
                .build();

        if ("DEPOSIT".equalsIgnoreCase(request.getType())) {
            Reservation reservation = reservationRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
            if (!reservation.getUser().getUserId().equals(user.getUserId())) {
                throw new BusinessRuleException("Access denied");
            }
            if (!"Pending".equals(reservation.getStatus())) {
                throw new BusinessRuleException("Reservation is not pending");
            }
            amount = reservation.getDepositAmount().longValue();
            description = "Coc dat cho " + reservation.getLicensePlate();
            payment.setAmount(BigDecimal.valueOf(amount));
            payment.setReservation(reservation);
        } else if ("PARKING".equalsIgnoreCase(request.getType())) {
            ParkingSession session = sessionRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
            if (session.getDriver() == null || !session.getDriver().getUserId().equals(user.getUserId())) {
                throw new BusinessRuleException("Access denied");
            }
            if (!"Admitted".equals(session.getStatus()) && !"Parked".equals(session.getStatus())) {
                throw new BusinessRuleException("Cannot pay for session in status: " + session.getStatus());
            }
            BigDecimal fee = pricingService.calculateFee(session.getVehicleType().getVehicleTypeId(), session.getEntryTime(), LocalDateTime.now());
            amount = fee.longValue();
            description = "Phi gui xe " + session.getLicensePlateIn();
            payment.setAmount(fee);
            payment.setSession(session);
        } else {
            throw new BusinessRuleException("Invalid payment type");
        }

        paymentRepository.save(payment);

        try {
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount > 0 ? amount : 2000L)
                    .description(description.length() > 25 ? description.substring(0, 25) : description)
                    .returnUrl("http://localhost:3000/payment/success")
                    .cancelUrl("http://localhost:3000/payment/cancel")
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);

            return PayosLinkResponse.builder()
                    .checkoutUrl(data.getCheckoutUrl())
                    .qrCode(data.getQrCode())
                    .orderCode(orderCode)
                    .amount(amount)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating PayOS link: " + e.getMessage());
        }
    }

    public void handlePayosWebhook(WebhookData webhookData) {
        if (!"00".equals(webhookData.getCode())) {
            return;
        }
        
        String orderCode = String.valueOf(webhookData.getOrderCode());
        Payment payment = paymentRepository.findFirstByTransactionReference(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for orderCode: " + orderCode));

        if ("Success".equals(payment.getPaymentStatus())) {
            return;
        }

        payment.setPaymentStatus("Success");
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        if (payment.getReservation() != null) {
            Reservation reservation = payment.getReservation();
            reservation.setDepositStatus("Paid");
            reservation.setStatus("Confirmed");
            reservationRepository.save(reservation);
        } else if (payment.getSession() != null) {
            ParkingSession session = payment.getSession();
            session.setStatus("Completed");
            session.setExitTime(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }
}
