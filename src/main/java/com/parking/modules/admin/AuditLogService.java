package com.parking.modules.admin;

import com.parking.common.PageResponse;
import com.parking.entity.AuditLog;
import com.parking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    /** Man "xem toan bo": phan trang, moi nhat truoc. size gioi han de tranh payload qua lon. */
    public PageResponse<AuditLog> findAll(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return PageResponse.of(auditLogRepository.findAll(pageable));
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
