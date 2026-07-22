package com.parking.repository;

import com.parking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser_UserId(Long userId);

    long countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            Integer vehicleTypeId, List<String> statuses, LocalDateTime end, LocalDateTime start);

    List<Reservation> findByLicensePlateAndVehicleType_VehicleTypeIdAndStatusInAndExpectedExitTimeGreaterThanEqual(
            String licensePlate, Integer vehicleTypeId, List<String> statuses, LocalDateTime time);

    long countByUser_UserIdAndStatusIn(Long userId, List<String> statuses);

    List<Reservation> findByLicensePlateAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            String licensePlate, List<String> statuses, LocalDateTime end, LocalDateTime start);
}
