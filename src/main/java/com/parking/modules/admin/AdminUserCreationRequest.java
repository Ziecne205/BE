package com.parking.modules.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreationRequest {

    @NotBlank(message = "Username khong duoc de trong")
    private String username;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 6, message = "Mat khau phai co it nhat 6 ky tu")
    private String password;

    @NotBlank(message = "Ho va ten khong duoc de trong")
    private String fullName;

    private String phoneNumber;

    @Email(message = "Email khong hop le")
    private String email;

    // Admin duoc phep tao them Admin khac; Manager (endpoint dung chung cho ca hai) CHI duoc tao
    // Manager/Staff/Driver — DTO khong the phan biet actor la Admin hay Manager, nen chan them o
    // UserAdminService.createUser (kiem tra role cua nguoi goi truoc khi cho tao Admin).
    @NotBlank(message = "Role khong duoc de trong")
    @Pattern(regexp = "^(Admin|Manager|Staff|Driver)$", message = "Role phai la Admin, Manager, Staff, hoac Driver")
    private String roleName;
}
