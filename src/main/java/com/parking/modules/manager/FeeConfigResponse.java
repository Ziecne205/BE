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
    /** So phut cho phep thanh toan coc ke tu luc tao booking; qua han -> tu dong Expired. */
    private Integer depositPaymentWindowMinutes;
    /** Muc lech (VND) cho phep giua tien mat Staff thu va so he thong tinh truoc khi can Manager duyet. */
    private BigDecimal cashToleranceVnd;
}
