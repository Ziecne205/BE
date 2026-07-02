package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token (OTP) khong duoc de trong")
    private String token;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    private String newPassword;
}
