package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token khong duoc de trong")
    private String token;

    // Chinh sach do dai (6-50) do service kiem tra de tra ve errorCode VALIDATION_ERROR dung hop dong.
    private String newPassword;
}
