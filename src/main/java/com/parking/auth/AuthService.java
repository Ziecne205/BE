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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.parking.common.service.EmailService;
import com.parking.entity.PasswordResetToken;
import com.parking.repository.PasswordResetTokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
public class AuthService {

    private static final int OTP_TTL_MINUTES = 10;
    private static final int REG_OTP_TTL_MINUTES = 10;
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

    /**
     * OTP dang ky luu TAM trong bo nho — KHONG tao User cho toi khi OTP duoc xac thuc. Key theo email.
     * Chu y dung bang DB de: (1) tranh migration cho ddl-auto: validate, (2) khong de lai User "rac"
     * chua xac thuc lam email/username bi "chiem". Danh doi: pending mat khi restart BE (OK cho dev).
     */
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    /** Du lieu dang ky dang cho xac thuc OTP (mat khau da bam san). */
    private record PendingRegistration(String username, String passwordHash, String fullName,
                                       String phoneNumber, String email, String otp,
                                       LocalDateTime expiresAt) {
    }

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

        String roleName = (request.getRoleName() == null || request.getRoleName().isBlank())
                ? "Driver"
                : request.getRoleName();
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role khong hop le: " + roleName));

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

    // ── Khoi phuc mat khau bang OTP — TACH 2 KENH theo vai tro ────────────────────────────────
    // Kenh KHACH (Driver): app tai xe (parking-driver) goi /auth/forgot-password + /auth/reset-password.
    // Kenh NOI BO (Staff/Manager/Admin): cong quan tri (parking-fe) goi /auth/staff/forgot-password +
    // /auth/staff/reset-password. Moi kenh chi phuc vu dung nhom vai tro cua no, nen app tai xe cong
    // khai KHONG the dat lai mat khau tai khoan noi bo (va nguoc lai). Bao mat cot loi van la kiem
    // soat hop thu email nhan OTP; viec tach kenh loai bo app khach khoi be mat tan cong tai khoan quan tri.

    /** Kenh KHACH: bat dau khoi phuc mat khau tai khoan Driver — tra ve email da che (n***@gmail.com). */
    public String processForgotPassword(String identifier) {
        return startReset(identifier, true);
    }

    /** Kenh KHACH: dat lai mat khau tai khoan Driver bang OTP (dung 1 lan). */
    public void resetPasswordWithOtp(String otp, String newPassword) {
        finishReset(otp, newPassword, true);
    }

    /** Kenh NOI BO: bat dau khoi phuc mat khau tai khoan Staff/Manager/Admin — tra ve email da che. */
    public String processStaffForgotPassword(String identifier) {
        return startReset(identifier, false);
    }

    /** Kenh NOI BO: dat lai mat khau tai khoan Staff/Manager/Admin bang OTP (dung 1 lan). */
    public void resetStaffPasswordWithOtp(String otp, String newPassword) {
        finishReset(otp, newPassword, false);
    }

    /**
     * Sinh + gui OTP dat lai mat khau, GIOI HAN theo dung kenh: {@code driverChannel=true} chi phuc vu
     * tai khoan Driver, {@code false} chi phuc vu Staff/Manager/Admin. Dung sai kenh -> tu choi (tranh
     * dung app khach de dat lai mat khau tai khoan noi bo).
     */
    private String startReset(String identifier, boolean driverChannel) {
        User user = userRepository.findByUsernameOrEmailOrPhone(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay tai khoan voi thong tin nay"));

        if (isDriver(user) != driverChannel) {
            throw new BusinessRuleException(wrongChannelMessage(driverChannel), "SELF_RESET_WRONG_CHANNEL");
        }

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
        log.info("Da gui OTP dat lai mat khau cho user {} (kenh {})",
                user.getUsername(), driverChannel ? "khach" : "noi bo");
        return maskEmail(user.getEmail());
    }

    /** Dat lai mat khau bang OTP (dung 1 lan), rang buoc dung kenh da phat hanh (khach vs noi bo). */
    private void finishReset(String otp, String newPassword, boolean driverChannel) {
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
        // Phong thu 2 lop: OTP chi dung duoc dung kenh da phat hanh cho no.
        if (isDriver(user) != driverChannel) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessRuleException(wrongChannelMessage(driverChannel), "SELF_RESET_WRONG_CHANNEL");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // OTP dung mot lan: xoa sau khi doi mat khau thanh cong.
        passwordResetTokenRepository.delete(resetToken);
    }

    private String wrongChannelMessage(boolean driverChannel) {
        return driverChannel
                ? "Tai khoan noi bo (nhan vien/quan ly) vui long dat lai mat khau tai cong quan tri."
                : "Tai khoan khach hang vui long dat lai mat khau tren ung dung tai xe.";
    }

    /**
     * Bat dau dang ky bang OTP email: validate trung username/email, sinh OTP, luu TAM (chua tao User)
     * va gui OTP toi email. Tra ve email da che de FE hien "da gui ma toi n***@gmail.com".
     */
    public String startRegistration(RegisterRequest request) {
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email khong duoc de trong", "VALIDATION_ERROR");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username da ton tai");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email da duoc su dung");
        }

        purgeExpiredRegistrations();
        String otp = randomOtp();
        pendingRegistrations.put(email.toLowerCase(), new PendingRegistration(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber(),
                email,
                otp,
                LocalDateTime.now().plusMinutes(REG_OTP_TTL_MINUTES)));

        emailService.sendRegistrationOtp(email, otp, REG_OTP_TTL_MINUTES);
        log.info("Da gui OTP dang ky cho email {}", email);
        return maskEmail(email);
    }

    /**
     * Xac thuc OTP dang ky -> tao User (role Driver, Active) va tra JWT de tu dong dang nhap.
     * OTP dung mot lan; xoa pending sau khi tao thanh cong.
     */
    public LoginResponse verifyRegistration(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email khong duoc de trong", "VALIDATION_ERROR");
        }
        purgeExpiredRegistrations();
        String key = email.toLowerCase();
        PendingRegistration pending = pendingRegistrations.get(key);
        if (pending == null) {
            throw new BadRequestException(
                    "Khong tim thay yeu cau dang ky cho email nay, vui long dang ky lai",
                    "NO_PENDING_REGISTRATION");
        }
        if (pending.expiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrations.remove(key);
            throw new BadRequestException("Ma OTP da het han, vui long dang ky lai", "OTP_EXPIRED");
        }
        if (!pending.otp().equals(otp)) {
            throw new BadRequestException("Ma OTP khong hop le", "INVALID_OTP");
        }
        // Trong luc cho OTP, username/email co the da bi dang ky boi mot luong khac.
        if (userRepository.existsByUsername(pending.username()) || userRepository.existsByEmail(pending.email())) {
            pendingRegistrations.remove(key);
            throw new BusinessRuleException("Tai khoan da ton tai");
        }

        Role role = roleRepository.findByRoleName("Driver")
                .orElseThrow(() -> new BusinessRuleException("He thong chua cau hinh Role Driver"));

        User user = User.builder()
                .username(pending.username())
                .passwordHash(pending.passwordHash())
                .fullName(pending.fullName())
                .phoneNumber(pending.phoneNumber())
                .email(pending.email())
                .role(role)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        pendingRegistrations.remove(key);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(token, user.getUsername(), role.getRoleName());
    }

    /** OTP 6 so ngau nhien. */
    private String randomOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    /** Tai khoan co phai KHACH (Driver) hay khong — dung de gioi han kenh dat lai mat khau. */
    private boolean isDriver(User user) {
        return user.getRole() != null && "Driver".equalsIgnoreCase(user.getRole().getRoleName());
    }

    /** OTP 6 so, duy nhat toan cuc (cot Token co rang buoc UNIQUE) — sinh lai neu trung. */
    private String generateUniqueOtp() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String otp = randomOtp();
            if (passwordResetTokenRepository.findByToken(otp).isEmpty()) {
                return otp;
            }
        }
        throw new BusinessRuleException("Khong the tao ma OTP, vui long thu lai", "OTP_GENERATION_FAILED");
    }

    /** Don cac pending registration da het han (goi truoc moi lan doc/ghi). */
    private void purgeExpiredRegistrations() {
        LocalDateTime now = LocalDateTime.now();
        pendingRegistrations.values().removeIf(p -> p.expiresAt().isBefore(now));
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
