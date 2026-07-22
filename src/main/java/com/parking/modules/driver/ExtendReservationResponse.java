package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ket qua gia han booking: booking da cap nhat expectedExitTime + link PayOS de thanh toan
 * phan gia han (Phase 5 — gia han tinh phi theo gia hien hanh, khong con mien phi). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendReservationResponse {
    private ReservationDTO reservation;
    private PayosLinkResponse payment;
}
