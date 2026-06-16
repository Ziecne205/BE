package com.parking.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username khong duoc trong")
    private String username;

    @NotBlank(message = "Password khong duoc trong")
    private String password;
}
