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
public class PaymentRequest {
    private Long sessionId;
    private String paymentMethod; // e.g., VNPay, Momo
    private BigDecimal amount; // (Optional) can verify from FE or recalculate in BE
}
