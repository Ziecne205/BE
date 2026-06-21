package com.parking.repository;

import com.parking.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityName(String entityName);
    List<AuditLog> findByUser_UserId(Long userId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
