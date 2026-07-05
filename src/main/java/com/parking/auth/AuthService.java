package com.parking.auth;

import com.parking.common.exception.BadRequestException;
import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.config.JwtService;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.RoleRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import com.parking.common.service.EmailService;
import com.parking.entity.PasswordResetToken;
import com.parking.repository.PasswordResetTokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class AuthService {

    private static final int RESET_TOKEN_TTL_MINUTES = 15;
    private static final int PASSWORD_MIN_LEN = 6;
    private static final int PASSWORD_MAX_LEN = 50;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** URL goc cua FE, dung de dung link dat lai mat khau trong email. */
    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.parking.config.AppUserDetailsService userDetailsService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));

        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(token, user.getUsername(), user.getRole().getRoleName());
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username da ton tai");
        }

        // Force role Driver for all public registrations
        Role role = roleRepository.findByRoleName("Driver")
                .orElseThrow(() -> new BusinessRuleException("He thong chua cau hinh Role Driver"));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .role(role)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(token, user.getUsername(), role.getRoleName());
    }

    /**
     * Bat dau luong tu khoi phuc mat khau. Tra ve void va KHONG bao loi neu tai khoan khong ton tai
     * (anti-enumeration): controller luon tra 200 + thong bao chung. Token la chuoi ngau nhien do dai
     * cao (khong phai OTP 6 so) de tranh do doan, gui qua email (out-of-band).
     */
    public void processForgotPassword(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.info("Yeu cau quen mat khau cho username khong ton tai (bo qua): {}", username);
            return; // Khong tiet lo tai khoan co ton tai hay khong.
        }
        User user = userOpt.get();

        // Chi giu token moi nhat cho moi user.
        passwordResetTokenRepository.deleteByUser_UserId(user.getUserId());

        String token = generateSecureToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES))
                .build());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } else {
            // Khong co kenh gui -> khong the chuyen token ra ngoai. Van im lang de chong enumeration.
            log.warn("Tai khoan {} khong co email — khong the gui link dat lai mat khau", username);
        }
    }

    public void resetPasswordWithToken(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < PASSWORD_MIN_LEN || newPassword.length() > PASSWORD_MAX_LEN) {
            throw new BadRequestException(
                    "Mật khẩu phải có từ " + PASSWORD_MIN_LEN + " đến " + PASSWORD_MAX_LEN + " ký tự",
                    "VALIDATION_ERROR");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Liên kết đặt lại không hợp lệ hoặc đã hết hạn", "INVALID_TOKEN"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Liên kết đặt lại không hợp lệ hoặc đã hết hạn", "TOKEN_EXPIRED");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Token dung mot lan: xoa sau khi doi mat khau thanh cong.
        passwordResetTokenRepository.delete(resetToken);
    }

    /** Token ngau nhien 256-bit, ma hoa URL-safe Base64 — khong the do doan (thay cho OTP 6 so). */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
