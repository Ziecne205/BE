package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Uoc tinh phi + tien coc cho mot khung gio dat (KHONG tao booking). Cho FE hien thi truoc khi
 * dat cho — moi con so deu do BE tinh (theo bang gia + cau hinh cua Manager) de FE khong phai
 * lap lai cong thuc gia.
 */
@Data
@AllArgsConstructor
public class ReservationQuoteDTO {
    private BigDecimal estimatedFee;
    private BigDecimal depositAmount;
}
