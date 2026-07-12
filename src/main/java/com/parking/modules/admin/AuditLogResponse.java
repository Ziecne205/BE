package com.parking.modules.admin;

import com.parking.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {

    private Long logId;
    private Long userId;
    private String username;
    private String action;
    private String entityName;
    private String entityId;
    private String detail;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .userId(log.getUser() != null ? log.getUser().getUserId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : null)
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
