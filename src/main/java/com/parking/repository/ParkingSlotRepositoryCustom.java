package com.parking.repository;

import java.util.List;

public interface ParkingSlotRepositoryCustom {
    /** Tong + con trong theo tung loai xe (thay cho vong lap 2N count). */
    List<SlotCountByType> countSlotsGroupedByVehicleType();
}
