package com.parking.repository;

import com.parking.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findFirstByLicensePlateInAndStatusIn(String licensePlateIn, List<String> statuses);
    List<ParkingSession> findByStatusIn(List<String> statuses);
    List<ParkingSession> findByDriver_UserId(Long userId);
    long countByVehicleType_VehicleTypeIdAndStatusIn(Integer vehicleTypeId, List<String> statuses);
}
