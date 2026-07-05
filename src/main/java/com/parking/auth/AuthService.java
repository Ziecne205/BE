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

import com.parking.common.service.EmailService;
import com.parking.entity.PasswordResetToken;
import com.parking.repository.PasswordResetTokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class AuthService {

    private static final int OTP_TTL_MINUTES = 10;
    private static final int PASSWORD_MIN_LEN = 6;
    private static final int PASSWORD_MAX_LEN = 50;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        // request.getUsername() co the la username / email / SDT — resolve bang cung 1 truy van.
        User user = userRepository.findByUsernameOrEmailOrPhone(request.getUsername()).stream().findFirst()
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
     * Bat dau luong khoi phuc mat khau bang OTP. Nhan dinh danh (username / email / SDT), sinh ma
     * OTP 6 so gui toi email da dang ky cua tai khoan. Tra ve email da che (masked) de FE hien
     * "da gui ma toi n***@gmail.com". Khac voi truoc: KHONG con anti-enumeration — bao loi ro rang
     * neu khong tim thay tai khoan / tai khoan chua co email (theo yeu cau nghiep vu cho demo).
     */
    public String processForgotPassword(String identifier) {
        User user = userRepository.findByUsernameOrEmailOrPhone(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay tai khoan voi thong tin nay"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessRuleException("Tai khoan chua co email de nhan ma OTP", "NO_EMAIL_ON_ACCOUNT");
        }

        // Chi giu OTP moi nhat cho moi user.
        passwordResetTokenRepository.deleteByUser_UserId(user.getUserId());

        String otp = generateUniqueOtp();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .build());

        emailService.sendPasswordResetOtp(user.getEmail(), otp, OTP_TTL_MINUTES);
        log.info("Da gui OTP dat lai mat khau cho user {}", user.getUsername());
        return maskEmail(user.getEmail());
    }

    /** Dat lai mat khau bang OTP (dung 1 lan). OTP la duy nhat toan cuc nen du de xac dinh user. */
    public void resetPasswordWithOtp(String otp, String newPassword) {
        if (newPassword == null || newPassword.length() < PASSWORD_MIN_LEN || newPassword.length() > PASSWORD_MAX_LEN) {
            throw new BadRequestException(
                    "Mật khẩu phải có từ " + PASSWORD_MIN_LEN + " đến " + PASSWORD_MAX_LEN + " ký tự",
                    "VALIDATION_ERROR");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(otp)
                .orElseThrow(() -> new BadRequestException(
                        "Mã OTP không hợp lệ", "INVALID_OTP"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới", "OTP_EXPIRED");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // OTP dung mot lan: xoa sau khi doi mat khau thanh cong.
        passwordResetTokenRepository.delete(resetToken);
    }

    /** OTP 6 so, duy nhat toan cuc (cot Token co rang buoc UNIQUE) — sinh lai neu trung. */
    private String generateUniqueOtp() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            if (passwordResetTokenRepository.findByToken(otp).isEmpty()) {
                return otp;
            }
        }
        throw new BusinessRuleException("Khong the tao ma OTP, vui long thu lai", "OTP_GENERATION_FAILED");
    }

    /** "nguyenkhoi2004vt@gmail.com" -> "ngu***@gmail.com" (che phan dau de tranh lo email day du). */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String name = email.substring(0, at);
        String shown = name.length() <= 3 ? name.substring(0, 1) : name.substring(0, 3);
        return shown + "***" + email.substring(at);
    }
}
