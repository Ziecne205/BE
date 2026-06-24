package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingInfoDTO {
    private String vehicleTypeName;
    private BigDecimal basePrice;
    private Integer baseHours;
    private BigDecimal extraHourPrice;
    private BigDecimal nightSurcharge;
}
