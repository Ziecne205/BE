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

    @Query(value = "{ 'reservation.$id': ?0 }", sort = "{ 'paymentId': -1 }")
    Optional<Payment> findFirstByReservation_ReservationIdOrderByPaymentIdDesc(UUID reservationId);
}
