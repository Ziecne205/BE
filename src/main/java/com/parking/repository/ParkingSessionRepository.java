package com.parking.repository;

import com.parking.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    @Query("SELECT p FROM ParkingSession p WHERE p.licensePlateIn = :licensePlateIn AND p.status IN :statuses")
    Optional<ParkingSession> findFirstByLicensePlateInAndStatusIn(@org.springframework.data.repository.query.Param("licensePlateIn") String licensePlateIn, @org.springframework.data.repository.query.Param("statuses") List<String> statuses);
    List<ParkingSession> findByDriver_UserId(Long userId);
    long countByVehicleType_VehicleTypeIdAndStatusIn(Integer vehicleTypeId, List<String> statuses);
    long countByStatusIn(List<String> statuses);
    List<ParkingSession> findByEntryTimeBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
    List<ParkingSession> findByStatusAndEntryTimeBetween(String status, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
