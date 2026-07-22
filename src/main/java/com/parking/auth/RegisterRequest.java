package com.parking.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    // SDT phai dung dung 10 chu so (khong khoang trang / ky tu khac).
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "\\d{10}", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    // KHONG them truong roleName vao DTO nay. Dang ky cong khai LUON tao tai khoan Driver
    // (ep cung trong AuthService.verifyRegistration). Tai khoan Staff/Manager/Admin chi duoc
    // Admin tao qua UserAdminController (Admin-only). Cho client tu chon roleName tren endpoint
    // /auth/** cong khai = lo hong leo thang dac quyen (ai cung dang ky duoc tai khoan Admin).
}
