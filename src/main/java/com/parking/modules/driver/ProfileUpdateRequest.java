package com.parking.modules.driver;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "Ho ten khong duoc de trong")
    private String fullName;

    private String phoneNumber;

    @Email(message = "Email khong hop le")
    private String email;
}
