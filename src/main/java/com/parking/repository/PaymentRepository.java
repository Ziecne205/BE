package com.parking.repository;

import com.parking.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends MongoRepository<Payment, Long>, PaymentRepositoryCustom {

    List<Payment> findByPaymentTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("{ 'session.$id': ?0 }")
    List<Payment> findBySession_SessionId(Long sessionId);

    Optional<Payment> findFirstByTransactionReference(String transactionReference);

    /** Hang cho xu ly hoan tien thu cong (Phase 6.1) — vd refundStatus="ManualRequired". */
    List<Payment> findByRefundStatus(String refundStatus);

    @Query(value = "{ 'reservation.$id': ?0 }", sort = "{ 'paymentId': -1 }")
    Optional<Payment> findFirstByReservation_ReservationIdOrderByPaymentIdDesc(UUID reservationId);

    /** Cac Payment (bat ke trang thai) khoa vao mot reservation theo muc dich cu the — dung de
     * cong don extension_charge vao total_fee (Phase 4), bat ke da Success hay con Pending. */
    @Query("{ 'reservation.$id': ?0, 'paymentPurpose': ?1 }")
    List<Payment> findByReservation_ReservationIdAndPaymentPurpose(UUID reservationId, String paymentPurpose);

    /** Cac Payment da Success cua MOT session, loc theo muc dich — dung de doi soat checkout
     * (Phase 4: sum thay vi findFirst, de khong bo sot khi co nhieu giao dich Fee/Extension). */
    @Query("{ 'session.$id': ?0, 'paymentStatus': ?1, 'paymentPurpose': { $in: ?2 } }")
    List<Payment> findBySession_SessionIdAndPaymentStatusAndPaymentPurposeIn(
            Long sessionId, String paymentStatus, List<String> paymentPurposes);

    /** Nhu tren nhung theo reservation — dung cho Extension payment (khong gan voi 1 session cu
     * the, vi co the duoc tao truoc khi check-in). */
    @Query("{ 'reservation.$id': ?0, 'paymentStatus': ?1, 'paymentPurpose': { $in: ?2 } }")
    List<Payment> findByReservation_ReservationIdAndPaymentStatusAndPaymentPurposeIn(
            UUID reservationId, String paymentStatus, List<String> paymentPurposes);
}
