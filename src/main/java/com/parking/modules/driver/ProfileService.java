package com.parking.modules.driver;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ho so tai khoan cho nguoi dung dang nhap (lay theo username tu JWT).
 * Dung chung cho tai xe; cac role khac cung xem/sua duoc ho so cua chinh minh.
 */
@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class ProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileDTO getProfile(String username) {
        return toDto(findByUsername(username));
    }

    @Transactional
    public ProfileDTO updateProfile(String username, ProfileUpdateRequest request) {
        User user = findByUsername(username);
        user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return toDto(userRepository.save(user));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
    }

    private ProfileDTO toDto(User user) {
        return ProfileDTO.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .status(user.getStatus())
                .build();
    }
}
