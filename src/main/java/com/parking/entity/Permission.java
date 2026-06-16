package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PermissionID")
    private Integer permissionId;

    @Column(name = "PermissionCode", nullable = false, unique = true)
    private String permissionCode;

    @Column(name = "Description")
    private String description;
}
