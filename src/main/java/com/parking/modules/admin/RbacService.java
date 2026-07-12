package com.parking.modules.admin;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.Permission;
import com.parking.entity.Role;
import com.parking.repository.PermissionRepository;
import com.parking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogWriter auditLogService;

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    public Role findRoleById(Integer id) {
        return roleRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay role #" + id));
    }

    public List<Permission> findAllPermissions() {
        return permissionRepository.findAll();
    }

    public Set<Permission> getPermissionsByRole(Integer roleId) {
        Role role = findRoleById(roleId);
        role.getPermissions().size();
        return role.getPermissions();
    }

    @Transactional
    public Role assignPermissions(Integer roleId, Set<Integer> permissionIds, String actorUsername) {
        Role role = findRoleById(roleId);
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("Mot so permission ID khong ton tai");
        }
        role.setPermissions(permissions);
        Role saved = roleRepository.save(role);
        auditLogService.log(actorUsername, "RBAC_ASSIGN_PERMISSIONS", "Role", String.valueOf(roleId),
                "Gan lai toan bo quyen cho role " + role.getRoleName() + ": " + permissionIds);
        return saved;
    }

    @Transactional
    public Role addPermission(Integer roleId, Integer permissionId, String actorUsername) {
        Role role = findRoleById(roleId);
        Permission permission = permissionRepository.findById(Objects.requireNonNull(permissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay permission #" + permissionId));
        role.getPermissions().add(permission);
        Role saved = roleRepository.save(role);
        auditLogService.log(actorUsername, "RBAC_ADD_PERMISSION", "Role", String.valueOf(roleId),
                "Them quyen " + permission.getPermissionCode() + " vao role " + role.getRoleName());
        return saved;
    }

    @Transactional
    public Role removePermission(Integer roleId, Integer permissionId, String actorUsername) {
        Role role = findRoleById(roleId);
        role.getPermissions().removeIf(p -> p.getPermissionId().equals(permissionId));
        Role saved = roleRepository.save(role);
        auditLogService.log(actorUsername, "RBAC_REMOVE_PERMISSION", "Role", String.valueOf(roleId),
                "Xoa quyen #" + permissionId + " khoi role " + role.getRoleName());
        return saved;
    }
}
