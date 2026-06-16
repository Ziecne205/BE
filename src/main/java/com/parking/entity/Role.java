package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer roleId;

    @Column(name = "RoleName", nullable = false, unique = true)
    private String roleName;

    @Column(name = "Description")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "RolePermissions",
            joinColumns = @JoinColumn(name = "RoleID"),
            inverseJoinColumns = @JoinColumn(name = "PermissionID")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
