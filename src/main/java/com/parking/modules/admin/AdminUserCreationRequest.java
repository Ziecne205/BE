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

    // KHONG cho tao tai khoan Admin qua API (tai khoan Admin do DB/seed cap) — chong leo thang
    // dac quyen. Admin va Manager (endpoint mo cho ca hai) deu tao duoc Manager/Staff/Driver.
    @NotBlank(message = "Role khong duoc de trong")
    @Pattern(regexp = "^(Manager|Staff|Driver)$", message = "Role phai la Manager, Staff, hoac Driver")
    private String roleName;
}
