package com.parking.repository;

import com.parking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySession_SessionId(Long sessionId);
    java.util.Optional<Payment> findFirstByTransactionReference(String transactionReference);
}
