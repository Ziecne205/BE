package com.parking.modules.admin;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import com.parking.common.exception.BusinessRuleException;
import com.parking.entity.Role;
import com.parking.repository.RoleRepository;

/**
 * Vi du CRUD cho phan he Quan tri vien (Admin) - User Management.
 * RBAC (Permissions/RolePermissions), AuditLog, SystemConfig lam theo cung
 * pattern nay.
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogWriter auditLogService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user #" + id));
    }

    public User updateStatus(Long id, String status, String actorUsername) {
        User user = findById(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_UPDATE_USER_STATUS", "User", String.valueOf(id),
                "Doi trang thai user " + user.getUsername() + " -> " + status);
        return saved;
    }

    public User resetPassword(Long id, String newPassword, String actorUsername) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_RESET_PASSWORD", "User", String.valueOf(id),
                "Reset mat khau cho user " + user.getUsername());
        return saved;
    }

    public User createUser(AdminUserCreationRequest request, String actorUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username da ton tai");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new BusinessRuleException("Role khong hop le: " + request.getRoleName()));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .role(role)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_CREATE_USER", "User", String.valueOf(saved.getUserId()),
                "Tao user moi " + saved.getUsername() + " (role " + role.getRoleName() + ")");
        return saved;
    }
}
