package com.parking.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Identifier khong duoc trong")
    @JsonAlias({"username", "email", "phoneNumber"})
    @JsonProperty("identifier")
    private String identifier;

    @NotBlank(message = "Mat khau moi khong duoc trong")
    private String newPassword;

}
