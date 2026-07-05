package com.parking.modules.manager;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FeeConfigRequest {
    private BigDecimal hourlyRate;

    @NotNull
    private BigDecimal depositPercent;

    @NotNull
    private BigDecimal overstayRatePerHour;

    @NotNull
    private Integer noShowGraceMinutes;

    @NotNull
    private Integer blacklistThreshold;

    /** Tuy chon: so phut cho phep thanh toan coc ke tu luc tao booking. Bo trong -> giu gia tri cu. */
    private Integer depositPaymentWindowMinutes;
}
