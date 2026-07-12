package com.parking.auth;

import com.parking.common.ApiResponse;
import com.parking.common.service.RateLimiterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Dang nhap / dang ky")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiter;

    // Cua so rate-limit chung. Muc gioi han dat theo tung nhom hanh dong ben duoi.
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // Chan brute-force mat khau: toi da 20 lan/IP/15 phut (du long cho dev test nhieu vai tro,
        // van chan brute-force can hang nghin luot).
        rateLimiter.checkRateLimit("login:" + clientIp(http), 20, WINDOW);
        return ApiResponse.ok(authService.login(request));
    }

    /** Buoc 1 dang ky co xac thuc email: gui OTP toi email, chua tao tai khoan. */
    @PostMapping("/register/send-otp")
    public ApiResponse<String> sendRegisterOtp(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        // Chan spam gui email OTP: toi da 5 lan/IP/15 phut.
        rateLimiter.checkRateLimit("register-otp:" + clientIp(http), 5, WINDOW);
        String maskedEmail = authService.startRegistration(request);
        return ApiResponse.ok("Đã gửi mã OTP xác nhận tới email của bạn.", maskedEmail);
    }

    /** Buoc 2: xac thuc OTP -> tao tai khoan Driver va tra JWT (tu dong dang nhap). */
    @PostMapping("/register/verify")
    public ApiResponse<LoginResponse> verifyRegister(@Valid @RequestBody VerifyRegistrationRequest request, HttpServletRequest http) {
        // Chan brute-force OTP dang ky: toi da 10 lan/IP/15 phut.
        rateLimiter.checkRateLimit("register-verify:" + clientIp(http), 10, WINDOW);
        return ApiResponse.ok("Đăng ký thành công", authService.verifyRegistration(request.getEmail(), request.getOtp()));
    }

    // ── Kenh KHACH (Driver) ──────────────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimitForgot(http, request.getIdentifier());
        // Tra ve email da che (n***@gmail.com) trong `data` de FE hien "da gui OTP toi ...".
        String maskedEmail = authService.processForgotPassword(request.getIdentifier());
        return ApiResponse.ok("Đã gửi mã OTP tới email của bạn.", maskedEmail);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        // Chan brute-force ma OTP 6 so: toi da 10 lan/IP/15 phut.
        rateLimiter.checkRateLimit("reset:" + clientIp(http), 10, WINDOW);
        authService.resetPasswordWithOtp(request.getOtp(), request.getNewPassword());
        return ApiResponse.ok("Mat khau da duoc dat lai thanh cong.", null);
    }

    // ── Kenh NOI BO (Staff/Manager/Admin) — tach rieng khoi kenh khach o tren ────────────────

    @PostMapping("/staff/forgot-password")
    public ApiResponse<String> staffForgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimitForgot(http, request.getIdentifier());
        String maskedEmail = authService.processStaffForgotPassword(request.getIdentifier());
        return ApiResponse.ok("Đã gửi mã OTP tới email của bạn.", maskedEmail);
    }

    @PostMapping("/staff/reset-password")
    public ApiResponse<Void> staffResetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        rateLimiter.checkRateLimit("reset:" + clientIp(http), 10, WINDOW);
        authService.resetStaffPasswordWithOtp(request.getOtp(), request.getNewPassword());
        return ApiResponse.ok("Mat khau da duoc dat lai thanh cong.", null);
    }

    /**
     * JWT stateless: server khong luu phien nen "dang xuat" = client tu xoa token.
     * Endpoint nay ton tai de FE goi (best-effort) va tra ve 200 nhat quan thay vi 404.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("Da dang xuat", null);
    }

    /**
     * Rate-limit yeu cau gui OTP quen mat khau tren CA HAI kenh: theo IP (5/15') chan mot nguon spam,
     * va theo dinh danh muc tieu (3/15') chan "email-bomb" nham vao 1 tai khoan cu the.
     */
    private void rateLimitForgot(HttpServletRequest http, String identifier) {
        rateLimiter.checkRateLimit("forgot-ip:" + clientIp(http), 5, WINDOW);
        if (identifier != null && !identifier.isBlank()) {
            rateLimiter.checkRateLimit("forgot-id:" + identifier.trim().toLowerCase(), 3, WINDOW);
        }
    }

    /** IP client that: uu tien X-Forwarded-For (khi sau reverse proxy), fallback ve remote addr. */
    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
