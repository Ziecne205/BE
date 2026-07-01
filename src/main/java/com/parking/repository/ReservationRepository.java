package com.parking.repository;

import com.parking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser_UserId(Long userId);

    long countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            Integer vehicleTypeId, List<String> statuses, LocalDateTime end, LocalDateTime start);

    long countByStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            List<String> statuses, LocalDateTime end, LocalDateTime start);

    List<Reservation> findByLicensePlateAndVehicleType_VehicleTypeIdAndStatusInAndExpectedExitTimeGreaterThanEqual(
            String licensePlate, Integer vehicleTypeId, List<String> statuses, LocalDateTime time);

    /** Booking van con o trang thai chua check-in (Pending/Confirmed) nhung da qua gio du kien ra -> khong den nhan xe (no-show). */
    List<Reservation> findByStatusInAndExpectedExitTimeBefore(List<String> statuses, LocalDateTime threshold);

    /** Danh sach booking dang "Outstanding" cho mot loai xe, moi nhat truoc (dung cho cascade khi o bao tri). */
    List<Reservation> findByVehicleType_VehicleTypeIdAndStatusInOrderByCreatedAtDesc(
            Integer vehicleTypeId, List<String> statuses);
}
