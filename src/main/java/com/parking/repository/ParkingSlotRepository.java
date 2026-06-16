package com.parking.repository;

import com.parking.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    List<ParkingSlot> findByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);
    long countByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);
    long countByVehicleType_VehicleTypeIdAndStatusNot(Integer vehicleTypeId, String status);
}
