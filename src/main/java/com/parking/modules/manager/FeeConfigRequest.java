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

    /** Tuy chon: so phut phai doi sau khi dat cho moi duoc phep huy. Bo trong -> giu gia tri cu. */
    private Integer cancelWindowMinutes;

    /** Tuy chon: muc lech (VND) cho phep khi thu tien mat truoc khi can Manager duyet. Bo trong -> giu gia tri cu. */
    private BigDecimal cashToleranceVnd;
}
