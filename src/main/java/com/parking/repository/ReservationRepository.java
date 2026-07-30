package com.parking.repository;

import com.parking.entity.Reservation;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends MongoRepository<Reservation, UUID> {

    @Query("{ 'user.$id': ?0 }")
    List<Reservation> findByUser_UserId(Long userId);

    @CountQuery("{ 'vehicleType.$id': ?0, 'status': { $in: ?1 }, 'expectedEntryTime': { $lt: ?2 }, 'expectedExitTime': { $gt: ?3 } }")
    long countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            Integer vehicleTypeId, List<String> statuses, LocalDateTime end, LocalDateTime start);

    long countByStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
            List<String> statuses, LocalDateTime end, LocalDateTime start);

    @Query("{ 'licensePlate': ?0, 'vehicleType.$id': ?1, 'status': { $in: ?2 }, 'expectedExitTime': { $gte: ?3 } }")
    List<Reservation> findByLicensePlateAndVehicleType_VehicleTypeIdAndStatusInAndExpectedExitTimeGreaterThanEqual(
            String licensePlate, Integer vehicleTypeId, List<String> statuses, LocalDateTime time);

    /** Booking chua check-in nhung da qua gio du kien ra -> no-show. */
    List<Reservation> findByStatusInAndExpectedExitTimeBefore(List<String> statuses, LocalDateTime threshold);

    /** Thay the ham tren o Phase 3: no-show tinh theo checkinDeadline (gia han theo bac) thay vi expectedExitTime thuan. */
    List<Reservation> findByStatusInAndCheckinDeadlineBefore(List<String> statuses, LocalDateTime threshold);

    /** Booking con "Pending" (chua thanh toan coc) qua han cua so thanh toan tinh tu luc tao. */
    List<Reservation> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);

    /** Danh sach booking dang "Outstanding" cho mot loai xe, moi nhat truoc. */
    @Query(value = "{ 'vehicleType.$id': ?0, 'status': { $in: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Reservation> findByVehicleType_VehicleTypeIdAndStatusInOrderByCreatedAtDesc(
            Integer vehicleTypeId, List<String> statuses);

    @CountQuery("{ 'user.$id': ?0, 'status': { $in: ?1 } }")
    long countByUser_UserIdAndStatusIn(Long userId, List<String> statuses);

    /** Booking cua mot khach dang o cac trang thai cho truoc — dung khi Admin khoa/ban tai khoan. */
    @Query("{ 'user.$id': ?0, 'status': { $in: ?1 } }")
    List<Reservation> findByUser_UserIdAndStatusIn(Long userId, List<String> statuses);

    @Query("{ 'licensePlate': ?0, 'status': { $in: ?1 }, 'expectedExitTime': { $gt: ?2 }, 'expectedEntryTime': { $lt: ?3 } }")
    List<Reservation> findByLicensePlateAndStatusInAndExpectedExitTimeGreaterThanAndExpectedEntryTimeLessThan(
            String licensePlate, List<String> statuses, LocalDateTime start, LocalDateTime end);
}
