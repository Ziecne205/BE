package com.parking.modules.manualrefund;

import com.parking.entity.ManualRefundRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ManualRefundResponse {
    private UUID id;
    private UUID reservationId;
    private String licensePlate;
    private String username;
    private String fullName;
    private String phoneNumber;
    private String email;
    private BigDecimal depositAmount;
    private String reason;
    private String bankInfo;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static ManualRefundResponse from(ManualRefundRequest request) {
        return ManualRefundResponse.builder()
                .id(request.getId())
                .reservationId(request.getReservation() != null ? request.getReservation().getReservationId() : null)
                .licensePlate(request.getReservation() != null ? request.getReservation().getLicensePlate() : null)
                .depositAmount(request.getReservation() != null ? request.getReservation().getDepositAmount() : null)
                .username(request.getUser() != null ? request.getUser().getUsername() : null)
                .fullName(request.getUser() != null ? request.getUser().getFullName() : null)
                .phoneNumber(request.getUser() != null ? request.getUser().getPhoneNumber() : null)
                .email(request.getUser() != null ? request.getUser().getEmail() : null)
                .reason(request.getReason())
                .bankInfo(request.getBankInfo())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }
}
