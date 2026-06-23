package com.parking.modules.manager;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PricingPolicyRequest {

    @NotNull(message = "Loai xe khong duoc de trong")
    private Integer vehicleTypeId;

    @NotNull(message = "Gia co ban khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia co ban phai lon hon 0")
    private BigDecimal basePrice;

    @NotNull(message = "So gio co ban khong duoc de trong")
    @Min(value = 1, message = "So gio co ban toi thieu la 1")
    private Integer baseHours;

    @NotNull(message = "Gia moi gio them khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia moi gio them phai lon hon 0")
    private BigDecimal extraHourPrice;

    /** Phu thu dem (18h-6h) - co the null neu khong tinh phu thu */
    private BigDecimal nightSurcharge;

    /** Phi mat ve - mac dinh 0 */
    private BigDecimal lostTicketFee = BigDecimal.ZERO;

    @NotNull(message = "Ngay hieu luc khong duoc de trong")
    private LocalDateTime effectiveDate;
}
