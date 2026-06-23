package com.parking.repository;

import com.parking.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsBySession_SessionId(Long sessionId);
}
