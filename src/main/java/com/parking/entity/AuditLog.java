package com.parking.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

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
