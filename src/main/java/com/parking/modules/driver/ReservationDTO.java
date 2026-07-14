package com.parking.modules.driver;

import com.parking.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phang cho dat cho — tranh tra nguyen Entity (keo theo User co passwordHash + lazy graph).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private UUID reservationId;
    private Long userId;
    private Integer vehicleTypeId;
    private String vehicleTypeName;
    private String licensePlate;
    private LocalDateTime expectedEntryTime;
    private LocalDateTime expectedExitTime;
    private BigDecimal depositAmount;
    private String depositStatus;
    private String status;
    private LocalDateTime createdAt;

    public static ReservationDTO from(Reservation r) {
        return ReservationDTO.builder()
                .reservationId(r.getReservationId())
                .userId(r.getUser() != null ? r.getUser().getUserId() : null)
                .vehicleTypeId(r.getVehicleType() != null ? r.getVehicleType().getVehicleTypeId() : null)
                .vehicleTypeName(r.getVehicleType() != null ? r.getVehicleType().getTypeName() : null)
                .licensePlate(r.getLicensePlate())
                .expectedEntryTime(r.getExpectedEntryTime())
                .expectedExitTime(r.getExpectedExitTime())
                .depositAmount(r.getDepositAmount())
                .depositStatus(r.getDepositStatus())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
