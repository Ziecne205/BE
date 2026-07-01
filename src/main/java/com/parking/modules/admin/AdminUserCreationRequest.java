package com.parking.modules.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserCreationRequest {

    @NotBlank(message = "Username khong duoc de trong")
    private String username;

    @NotBlank(message = "Mat khau khong duoc de trong")
    private String password;

    @NotBlank(message = "Ho va ten khong duoc de trong")
    private String fullName;

    private String phoneNumber;

    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "Role khong duoc de trong")
    @Pattern(regexp = "^(Manager|Staff|Driver)$", message = "Role phai la Manager, Staff, hoac Driver")
    private String roleName;
}
