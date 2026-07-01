package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ket qua tao link PayOS: URL trang thanh toan + chuoi QR (VietQR) de FE render. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayosLinkResponse {
    private String checkoutUrl;
    private String qrCode;
    private Long orderCode;
    private Long amount;
}
