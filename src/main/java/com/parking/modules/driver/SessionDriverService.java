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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionDriverService {

        private final ParkingSessionRepository sessionRepository;
        private final UserRepository userRepository;
        private final PricingService pricingService;
        private final PaymentRepository paymentRepository;

    public List<ParkingSessionDTO> getCurrentSessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> activeStatuses = Arrays.asList("Admitted", "Parked", "Moved");
        List<ParkingSession> sessions = sessionRepository.findByDriver_UserIdAndStatusIn(
                user.getUserId(), activeStatuses);

        return sessions.stream().map(session -> {
            BigDecimal estimatedFee = pricingService.calculateFee(
                    session.getVehicleType().getVehicleTypeId(),
                    session.getEntryTime(),
                    null   // null → uses now(), correct for in-progress sessions
            );
            return ParkingSessionDTO.builder()
                    .sessionId(session.getSessionId())
                    .licensePlateIn(session.getLicensePlateIn())
                    .licensePlateOut(session.getLicensePlateOut())
                    .vehicleTypeName(session.getVehicleType().getTypeName())
                    .entryTime(session.getEntryTime())
                    .exitTime(session.getExitTime())
                    .status(session.getStatus())
                    .estimatedFee(estimatedFee)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ParkingSessionDTO> getHistorySessions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> historyStatuses = Arrays.asList("Completed", "Exception");
        List<ParkingSession> sessions = sessionRepository.findByDriver_UserIdAndStatusIn(
                user.getUserId(), historyStatuses);

        return sessions.stream().map(session -> {
            // Exception sessions with no exitTime have no meaningful fee to show.
            // Passing null would fall back to "now" and produce a nonsensical
            // ever-growing number, so we return null instead.
            BigDecimal fee = null;
            if (!("Exception".equals(session.getStatus()) && session.getExitTime() == null)) {
                fee = pricingService.calculateFee(
                        session.getVehicleType().getVehicleTypeId(),
                        session.getEntryTime(),
                        session.getExitTime());
            }
            return ParkingSessionDTO.builder()
                    .sessionId(session.getSessionId())
                    .licensePlateIn(session.getLicensePlateIn())
                    .licensePlateOut(session.getLicensePlateOut())
                    .vehicleTypeName(session.getVehicleType().getTypeName())
                    .entryTime(session.getEntryTime())
                    .exitTime(session.getExitTime())
                    .status(session.getStatus())
                    .estimatedFee(fee)
                    .build();
        }).collect(Collectors.toList());
    }

    public SessionDetailResponse getSessionDetail(Long sessionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getDriver() == null
                || !session.getDriver().getUserId().equals(user.getUserId())) {
            throw new BusinessRuleException("Ban khong co quyen xem phien nay");
        }

        // Same null-fee rule as history: Exception + no exitTime → null
        BigDecimal totalFee = null;
        if (!("Exception".equals(session.getStatus()) && session.getExitTime() == null)) {
            totalFee = pricingService.calculateFee(
                    session.getVehicleType().getVehicleTypeId(),
                    session.getEntryTime(),
                    session.getExitTime());
        }

        SessionDetailResponse.SessionDetailResponseBuilder builder = SessionDetailResponse.builder()
                .sessionId(session.getSessionId())
                .licensePlateIn(session.getLicensePlateIn())
                .licensePlateOut(session.getLicensePlateOut())
                .vehicleTypeName(session.getVehicleType().getTypeName())
                .entryTime(session.getEntryTime())
                .exitTime(session.getExitTime())
                .status(session.getStatus())
                .entryImageUrl(session.getEntryImageUrl())
                .exitImageUrl(session.getExitImageUrl())
                .entryGateName(session.getEntryGate().getGateName())
                .exitGateName(session.getExitGate() != null ? session.getExitGate().getGateName() : null)
                .totalFee(totalFee);

        // Pick the most recent successful payment; fall back to any payment if none succeeded
        List<Payment> payments = paymentRepository.findBySession_SessionId(sessionId);
        payments.stream()
                .filter(p -> "Success".equals(p.getPaymentStatus()))
                .findFirst()
                .or(() -> payments.stream().findFirst())
                .ifPresent(payment -> builder
                        .paymentStatus(payment.getPaymentStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .paymentTime(payment.getPaymentTime()));

        return builder.build();
    }
}
