package com.parking.auth;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Dang nhap / dang ky")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Dang ky thanh cong", authService.register(request));
    }

    /** Buoc 1 dang ky co xac thuc email: gui OTP toi email, chua tao tai khoan. */
    @PostMapping("/register/send-otp")
    public ApiResponse<String> sendRegisterOtp(@Valid @RequestBody RegisterRequest request) {
        String maskedEmail = authService.startRegistration(request);
        return ApiResponse.ok("Đã gửi mã OTP xác nhận tới email của bạn.", maskedEmail);
    }

    /** Buoc 2: xac thuc OTP -> tao tai khoan Driver va tra JWT (tu dong dang nhap). */
    @PostMapping("/register/verify")
    public ApiResponse<LoginResponse> verifyRegister(@Valid @RequestBody VerifyRegistrationRequest request) {
        return ApiResponse.ok("Đăng ký thành công", authService.verifyRegistration(request.getEmail(), request.getOtp()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Tra ve email da che (n***@gmail.com) trong `data` de FE hien "da gui OTP toi ...".
        String maskedEmail = authService.processForgotPassword(request.getIdentifier());
        return ApiResponse.ok("Đã gửi mã OTP tới email của bạn.", maskedEmail);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithOtp(request.getOtp(), request.getNewPassword());
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
}
