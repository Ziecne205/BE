package com.parking.repository;

import com.parking.entity.Feedback;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackRepository extends MongoRepository<Feedback, Long> {
    @ExistsQuery("{ 'session.$id': ?0 }")
    boolean existsBySession_SessionId(Long sessionId);
}
