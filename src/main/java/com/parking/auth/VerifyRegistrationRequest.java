package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Xac nhan dang ky bang OTP da gui toi email. */
@Data
public class VerifyRegistrationRequest {
    @NotBlank(message = "Email khong duoc de trong")
    private String email;

    @NotBlank(message = "Ma OTP khong duoc de trong")
    private String otp;
}
