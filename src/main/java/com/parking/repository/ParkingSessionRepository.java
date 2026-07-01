package com.parking.repository;

import com.parking.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    @Query("SELECT p FROM ParkingSession p WHERE p.licensePlateIn = :licensePlateIn AND p.status IN :statuses")
    Optional<ParkingSession> findFirstByLicensePlateInAndStatusIn(@Param("licensePlateIn") String licensePlateIn, @Param("statuses") List<String> statuses);
    List<ParkingSession> findByStatusIn(List<String> statuses);
    List<ParkingSession> findByDriver_UserId(Long userId);
    List<ParkingSession> findByDriver_UserIdAndStatusIn(Long userId, List<String> statuses);
    long countByVehicleType_VehicleTypeIdAndStatusIn(Integer vehicleTypeId, List<String> statuses);
    long countByStatusIn(List<String> statuses);
    List<ParkingSession> findByEntryTimeBetween(LocalDateTime from, LocalDateTime to);
    List<ParkingSession> findByStatusAndEntryTimeBetween(String status, LocalDateTime from, LocalDateTime to);

    /** Phien "Admitted" qua lau khong co tien trien (chua duoc ghi o thuc te / check-out) -> nghi van bo xe/loiterer. */
    List<ParkingSession> findByStatusAndEntryTimeBefore(String status, LocalDateTime threshold);
}
