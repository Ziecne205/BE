package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Ten dang nhap khong duoc de trong")
    private String username;
}
