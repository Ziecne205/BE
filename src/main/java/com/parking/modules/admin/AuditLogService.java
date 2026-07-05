package com.parking.modules.admin;

import com.parking.entity.AuditLog;
import com.parking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /** Man "xem toan bo": 200 dong moi nhat (bang append-only, chan payload). */
    public List<AuditLog> findAll() {
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> findByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    public List<AuditLog> findByEntityName(String entityName) {
        return auditLogRepository.findByEntityName(entityName);
    }

    public List<AuditLog> findByUserId(Long userId) {
        return auditLogRepository.findByUser_UserId(userId);
    }

    public List<AuditLog> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        return auditLogRepository.findByCreatedAtBetween(from, to);
    }
}
