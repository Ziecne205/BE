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

    /**
     * AuditLogs la bang append-only (moi hanh dong ghi 1 dong) nen la bang duy nhat tang khong
     * gioi han. Chi lay 200 dong moi nhat cho man "xem toan bo" de payload luon bi chan — cac
     * bo loc theo action/entity/user/ngay van co endpoint rieng khi can tra cuu sau hon.
     */
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
}
