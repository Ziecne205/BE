package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    /** Username, email hoac so dien thoai cua tai khoan can dat lai mat khau. */
    @NotBlank(message = "Vui long nhap ten dang nhap, email hoac so dien thoai")
    private String identifier;
}
