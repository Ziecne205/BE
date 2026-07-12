package com.parking.modules.manager;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FeeConfigResponse {
    private BigDecimal hourlyRate;
    private BigDecimal depositPercent;
    private BigDecimal overstayRatePerHour;
    private Integer noShowGraceMinutes;
    private Integer blacklistThreshold;
}
