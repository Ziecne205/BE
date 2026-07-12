package com.parking.common.service;

import com.parking.entity.AuditLog;
import com.parking.entity.User;
import com.parking.repository.AuditLogRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Ghi audit log dung chung cho cac thao tac thay doi du lieu nhay cam (Admin/Manager). */
@Service
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String username, String action, String entityName, String entityId, String detail) {
        User user = username != null ? userRepository.findByUsername(username).orElse(null) : null;
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}
