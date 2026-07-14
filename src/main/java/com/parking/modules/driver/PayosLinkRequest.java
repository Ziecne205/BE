package com.parking.modules.driver;

import lombok.Data;

/**
 * Yeu cau tao link thanh toan PayOS.
 * type = "DEPOSIT" (id = reservationId, dang UUID) hoac "PARKING" (id = sessionId, dang so).
 * id khai bao String de chua duoc ca hai dang, parse theo type o noi xu ly.
 */
@Data
public class PayosLinkRequest {
    private String type;
    private String id;
}
