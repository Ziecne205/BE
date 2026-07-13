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

    // Cho phep ca 4 vai tro o muc DTO; AI duoc tao vai tro NAO do UserAdminService kiem soat
    // theo thu bac (chi duoc tao vai tro <= vai tro cua nguoi tao) — chong leo thang dac quyen.
    @NotBlank(message = "Role khong duoc de trong")
    @Pattern(regexp = "^(Admin|Manager|Staff|Driver)$", message = "Role phai la Admin, Manager, Staff, hoac Driver")
    private String roleName;
}
