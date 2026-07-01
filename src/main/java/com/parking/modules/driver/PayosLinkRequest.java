package com.parking.modules.driver;

import lombok.Data;

/**
 * Yeu cau tao link thanh toan PayOS.
 * type = "DEPOSIT" (id = reservationId) hoac "PARKING" (id = sessionId).
 */
@Data
public class PayosLinkRequest {
    private String type;
    private Long id;
}
