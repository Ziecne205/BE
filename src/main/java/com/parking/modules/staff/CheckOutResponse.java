package com.parking.modules.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CheckOutResponse {
    private Long sessionId;
    private long parkedMinutes;
    private BigDecimal amount;
}
