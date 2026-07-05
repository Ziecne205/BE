package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AuditLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Long logId;

    // @JsonIgnore + flat getter (cung pattern voi Reservation/IncidentReport): serialize ca
    // entity User se lo email/phone/hash VA gay loi Hibernate-proxy khi la lazy proxy. FE chi
    // can ten nguoi thuc hien.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    /** Ten day du nguoi thuc hien (null neu hanh dong do he thong tao). */
    public String getUserFullName() {
        return user != null ? user.getFullName() : null;
    }

    /** Username nguoi thuc hien (fallback khi khong co fullName). */
    public String getUserName() {
        return user != null ? user.getUsername() : null;
    }

    @Column(name = "Action", nullable = false)
    private String action;

    @Column(name = "EntityName", nullable = false)
    private String entityName;

    @Column(name = "EntityID")
    private String entityId;

    @Column(name = "Detail")
    private String detail;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}
