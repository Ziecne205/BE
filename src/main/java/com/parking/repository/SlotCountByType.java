package com.parking.repository;

/**
 * Projection: so o do theo tung loai xe (tong + con trong) — dung cho 1 query GROUP BY
 * thay cho vong lap 2N count trong ParkingInfoService.
 */
public interface SlotCountByType {
    Integer getVehicleTypeId();
    long getTotal();
    long getAvailable();
}
