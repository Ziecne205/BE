package com.parking.repository;

import com.parking.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    List<ParkingSlot> findByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);
    List<ParkingSlot> findByFloor_FloorId(Integer floorId);
    long countByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);
    long countByVehicleType_VehicleTypeIdAndStatusNot(Integer vehicleTypeId, String status);
    long countByStatus(String status);
    long countByFloor_FloorIdAndStatus(Integer floorId, String status);
    boolean existsBySlotCode(String slotCode);

    @Query("SELECT s FROM ParkingSlot s " +
           "LEFT JOIN FETCH s.floor " +
           "LEFT JOIN FETCH s.vehicleType " +
           "WHERE s.floor.floorId = :floorId")
    List<ParkingSlot> findByFloorIdWithDetails(@Param("floorId") Integer floorId);

    @Query("SELECT s FROM ParkingSlot s " +
           "LEFT JOIN FETCH s.floor f " +
           "LEFT JOIN FETCH f.dedicatedVehicleType " +
           "LEFT JOIN FETCH s.vehicleType " +
           "WHERE s.slotId = :id")
    Optional<ParkingSlot> findByIdWithDetails(@Param("id") Long id);
}
