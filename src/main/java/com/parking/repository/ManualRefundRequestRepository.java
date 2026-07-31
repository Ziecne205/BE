package com.parking.repository;

import com.parking.entity.ManualRefundRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManualRefundRequestRepository extends MongoRepository<ManualRefundRequest, UUID> {
    
    // For driver to view their own requests
    List<ManualRefundRequest> findByUser_UserIdOrderByRequestedAtDesc(Long userId);

    // For manager to view all requests
    List<ManualRefundRequest> findAllByOrderByRequestedAtDesc();

    // Check if a request already exists for a reservation to avoid duplicates
    boolean existsByReservation_ReservationId(UUID reservationId);
}
