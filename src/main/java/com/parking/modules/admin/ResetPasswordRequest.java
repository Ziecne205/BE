package com.parking.modules.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Mat khau moi khong duoc trong")
    @Size(min = 6, max = 100, message = "Mat khau moi phai co it nhat 6 ky tu")
    private String newPassword;
}
