package com.parking.repository;

import com.parking.entity.ParkingSlot;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository extends MongoRepository<ParkingSlot, Long>, ParkingSlotRepositoryCustom {

    @Query("{ 'vehicleType.$id': ?0, 'status': ?1 }")
    List<ParkingSlot> findByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);

    @Query("{ 'floor.$id': ?0 }")
    List<ParkingSlot> findByFloor_FloorId(Integer floorId);

    @CountQuery("{ 'vehicleType.$id': ?0, 'status': ?1 }")
    long countByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);

    @CountQuery("{ 'vehicleType.$id': ?0, 'status': { $ne: ?1 } }")
    long countByVehicleType_VehicleTypeIdAndStatusNot(Integer vehicleTypeId, String status);

    @CountQuery("{ 'vehicleType.$id': ?0 }")
    long countByVehicleType_VehicleTypeId(Integer vehicleTypeId);

    long countByStatus(String status);

    @CountQuery("{ 'floor.$id': ?0, 'status': ?1 }")
    long countByFloor_FloorIdAndStatus(Integer floorId, String status);

    boolean existsBySlotCode(String slotCode);
    Optional<ParkingSlot> findBySlotCode(String slotCode);

    @Query("{ 'floor.$id': ?0 }")
    List<ParkingSlot> findByFloorIdWithDetails(Integer floorId);

    /** DBRef floor/vehicleType tu load (eager) nen findById da du chi tiet. */
    default Optional<ParkingSlot> findByIdWithDetails(Long id) {
        return findById(id);
    }
}
