package com.parking.modules.staff;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dat cho chua check-in (Pending/Confirmed) hien thi truoc tren man "Phien hoat dong" de Staff
 * biet truoc xe nao se den va luc nao — khac voi ActiveSessionDto (chi phien DA check-in thuc su).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingReservationDto {
    private UUID reservationId;
    private String licensePlate;
    private String vehicleTypeName;
    private LocalDateTime expectedEntryTime;
    private LocalDateTime expectedExitTime;
    /** Pending (chua thanh toan coc) / Confirmed (da dat coc, cho den nhan xe). */
    private String status;
    private String depositStatus;
    private BigDecimal estimatedFeeAtBooking;
}
