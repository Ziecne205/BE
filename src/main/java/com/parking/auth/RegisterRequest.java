package com.parking.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    private String phoneNumber;

    @Email
    private String email;

    // KHONG them truong roleName vao DTO nay. Dang ky cong khai LUON tao tai khoan Driver
    // (ep cung trong AuthService.verifyRegistration). Tai khoan Staff/Manager/Admin chi duoc
    // Admin tao qua UserAdminController (Admin-only). Cho client tu chon roleName tren endpoint
    // /auth/** cong khai = lo hong leo thang dac quyen (ai cung dang ky duoc tai khoan Admin).
}
