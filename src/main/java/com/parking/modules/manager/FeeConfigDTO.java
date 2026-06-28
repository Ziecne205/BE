package com.parking.modules.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeConfigDTO {
    private BigDecimal hourlyRate;
    private BigDecimal depositPercent;
    private BigDecimal overstayRatePerHour;
    private Integer noShowGraceMinutes;
    private Integer blacklistThreshold;
}
