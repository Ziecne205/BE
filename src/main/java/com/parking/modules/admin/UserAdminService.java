package com.parking.modules.admin;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vi du CRUD cho phan he Quan tri vien (Admin) - User Management.
 * RBAC (Permissions/RolePermissions), AuditLog, SystemConfig lam theo cung pattern nay.
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user #" + id));
    }

    public User updateStatus(Long id, String status) {
        User user = findById(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User resetPassword(Long id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
