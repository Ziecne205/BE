package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "AuditLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private Long logId;

    // @JsonIgnore + flat getter: serialize ca entity User se lo email/phone/hash. FE chi
    // can ten nguoi thuc hien.
    @JsonIgnore
    @DBRef
    private User user;

    /** Ten day du nguoi thuc hien (null neu hanh dong do he thong tao). */
    public String getUserFullName() {
        return user != null ? user.getFullName() : null;
    }

    /** Username nguoi thuc hien (fallback khi khong co fullName). */
    public String getUserName() {
        return user != null ? user.getUsername() : null;
    }

    private String action;

    private String entityName;

    private String entityId;

    private String detail;

    private LocalDateTime createdAt;
}
