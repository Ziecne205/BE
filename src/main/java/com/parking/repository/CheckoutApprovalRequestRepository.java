package com.parking.repository;

import com.parking.entity.CheckoutApprovalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CheckoutApprovalRequestRepository extends MongoRepository<CheckoutApprovalRequest, Long> {
    List<CheckoutApprovalRequest> findByStatus(String status);
}
