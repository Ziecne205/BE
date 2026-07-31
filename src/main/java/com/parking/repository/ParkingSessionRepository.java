package com.parking.repository;

import com.parking.entity.ParkingSession;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends MongoRepository<ParkingSession, Long> {

    @Query("{ 'licensePlateIn': ?0, 'status': { $in: ?1 } }")
    Optional<ParkingSession> findFirstByLicensePlateInAndStatusIn(String licensePlateIn, List<String> statuses);

    List<ParkingSession> findByStatusIn(List<String> statuses);

    @Query("{ 'driver.$id': ?0 }")
    List<ParkingSession> findByDriver_UserId(Long userId);

    @Query("{ 'driver.$id': ?0, 'status': { $in: ?1 } }")
    List<ParkingSession> findByDriver_UserIdAndStatusIn(Long userId, List<String> statuses);

    @CountQuery("{ 'vehicleType.$id': ?0, 'status': { $in: ?1 } }")
    long countByVehicleType_VehicleTypeIdAndStatusIn(Integer vehicleTypeId, List<String> statuses);

    long countByStatusIn(List<String> statuses);

    List<ParkingSession> findByEntryTimeBetween(LocalDateTime from, LocalDateTime to);

    List<ParkingSession> findByStatusAndEntryTimeBetween(String status, LocalDateTime from, LocalDateTime to);

    /** Phien "Admitted" qua lau khong co tien trien -> nghi van bo xe/loiterer. */
    List<ParkingSession> findByStatusAndEntryTimeBefore(String status, LocalDateTime threshold);

    /** Phien "Admitted" da qua moc giu o goi y (suggestedSlotHoldExpiresAt) -> ung vien tu dong
     * chuyen sang "Parked" (xem SessionExpiryScheduler.autoParkAdmittedSessions). */
    List<ParkingSession> findByStatusAndSuggestedSlotHoldExpiresAtBefore(String status, LocalDateTime threshold);

    /** Phien dang mo, chua bi danh dau overstay — ung vien cho flagOverstaySessions() (Phase 4).
     * Loc them theo reservation.expectedExitTime trong Java (DBRef khong the query xuyen field). */
    @Query("{ 'status': { $in: ?0 }, 'isOverstayFlagged': { $ne: true } }")
    List<ParkingSession> findByStatusInAndIsOverstayFlaggedNot(List<String> statuses);

    /** Dung de chan xoa mot Gate dang/da duoc tham chieu boi lich su phien. */
    @ExistsQuery("{ $or: [ { 'entryGate.$id': ?0 }, { 'exitGate.$id': ?1 } ] }")
    boolean existsByEntryGate_GateIdOrExitGate_GateId(Integer entryGateId, Integer exitGateId);
}
