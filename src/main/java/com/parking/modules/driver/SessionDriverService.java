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
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> activeStatuses = Arrays.asList("Admitted", "Parked", "Moved");
        List<ParkingSession> sessions = sessionRepository.findByDriver_UserIdAndStatusIn(user.getUserId(), activeStatuses);

        return sessions.stream().map(session -> {
            BigDecimal estimatedFee = pricingService.calculateFee(
                    session.getVehicleType().getVehicleTypeId(),
                    session.getEntryTime(),
                    null
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> historyStatuses = Arrays.asList("Completed", "Exception");
        List<ParkingSession> sessions = sessionRepository.findByDriver_UserIdAndStatusIn(user.getUserId(), historyStatuses);

        return sessions.stream().map(session -> {
            BigDecimal fee = pricingService.calculateFee(
                    session.getVehicleType().getVehicleTypeId(),
                    session.getEntryTime(),
                    session.getExitTime()
            );

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
                .orElseThrow(() -> new RuntimeException("User not found"));

        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getDriver() == null || !session.getDriver().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied");
        }

        BigDecimal totalFee = pricingService.calculateFee(
                session.getVehicleType().getVehicleTypeId(),
                session.getEntryTime(),
                session.getExitTime()
        );

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

        // Fetch payment
        paymentRepository.findBySession_SessionId(sessionId).stream().findFirst().ifPresent(payment -> {
            builder.paymentStatus(payment.getPaymentStatus())
                   .paymentMethod(payment.getPaymentMethod())
                   .paymentTime(payment.getPaymentTime());
        });

        return builder.build();
    }
}
