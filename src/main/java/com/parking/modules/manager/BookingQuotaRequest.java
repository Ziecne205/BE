package com.parking.modules.manager;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class BookingQuotaRequest {

    @NotNull(message = "Loai xe khong duoc de trong")
    private Integer vehicleTypeId;

    @NotNull(message = "Gio bat dau khung dat cho khong duoc de trong")
    private LocalTime startTime;

    @NotNull(message = "Gio ket thuc khung dat cho khong duoc de trong")
    private LocalTime endTime;

    /**
     * Phan tram toi da suc chua danh cho dat cho truoc (0-100%).
     * Vi du: 60 -> toi da 60% so o danh cho dat cho trong khung gio nay.
     */
    @NotNull(message = "Phan tram quota khong duoc de trong")
    @DecimalMin(value = "0.0", message = "QuotaPercent phai >= 0")
    @DecimalMax(value = "100.0", message = "QuotaPercent phai <= 100")
    private BigDecimal quotaPercent;
}
