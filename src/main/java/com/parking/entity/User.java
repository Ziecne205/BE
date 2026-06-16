package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long userId;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "PhoneNumber", unique = true)
    private String phoneNumber;

    @Column(name = "Email", unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleID", nullable = false)
    private Role role;

    @Column(name = "Status")
    private String status; // Active, Inactive, Banned

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
