package com.parking.repository;

import com.parking.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, Long> {
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityName(String entityName);

    @Query("{ 'user.$id': ?0 }")
    List<AuditLog> findByUser_UserId(Long userId);

    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /** So log da qua han luu tru — de bao cao truoc khi don. */
    long countByCreatedAtBefore(LocalDateTime cutoff);

    /** Xoa log cu hon moc (retention). Tra ve so document da xoa. */
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
