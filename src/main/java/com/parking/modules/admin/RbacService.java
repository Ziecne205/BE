package com.parking.modules.admin;

import com.parking.common.exception.ResourceNotFoundException;
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
@SuppressWarnings("null")
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

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
    public Role assignPermissions(Integer roleId, Set<Integer> permissionIds) {
        Role role = findRoleById(roleId);
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("Mot so permission ID khong ton tai");
        }
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    @Transactional
    public Role addPermission(Integer roleId, Integer permissionId) {
        Role role = findRoleById(roleId);
        Permission permission = permissionRepository.findById(Objects.requireNonNull(permissionId))
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay permission #" + permissionId));
        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermission(Integer roleId, Integer permissionId) {
        Role role = findRoleById(roleId);
        role.getPermissions().removeIf(p -> p.getPermissionId().equals(permissionId));
        return roleRepository.save(role);
    }
}
