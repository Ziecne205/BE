package com.parking.repository;

import com.parking.entity.ParkingSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhom o do theo loai xe bang Java (thay vi aggregation) — vi vehicleType luu duoi dang DBRef,
 * field path "$id" khong dung duoc trong aggregation pipeline. So luong o do co gioi han nen
 * duyet trong bo nho la chap nhan duoc va don gian, an toan hon.
 */
@RequiredArgsConstructor
public class ParkingSlotRepositoryImpl implements ParkingSlotRepositoryCustom {

    private final MongoOperations mongoOperations;

    @Override
    public List<SlotCountByType> countSlotsGroupedByVehicleType() {
        List<ParkingSlot> slots = mongoOperations.findAll(ParkingSlot.class);
        Map<Integer, long[]> agg = new LinkedHashMap<>(); // vehicleTypeId -> [total, available]

        for (ParkingSlot s : slots) {
            if (s.getVehicleType() == null) {
                continue;
            }
            Integer vtId = s.getVehicleType().getVehicleTypeId();
            long[] counts = agg.computeIfAbsent(vtId, k -> new long[2]);
            counts[0]++;
            if ("Available".equals(s.getStatus())) {
                counts[1]++;
            }
        }

        List<SlotCountByType> result = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : agg.entrySet()) {
            result.add(new SlotCountByTypeValue(e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        return result;
    }

    private record SlotCountByTypeValue(Integer vehicleTypeId, long total, long available)
            implements SlotCountByType {
        @Override
        public Integer getVehicleTypeId() {
            return vehicleTypeId;
        }

        @Override
        public long getTotal() {
            return total;
        }

        @Override
        public long getAvailable() {
            return available;
        }
    }
}
