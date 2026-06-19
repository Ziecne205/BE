package com.parking.modules.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    @NotBlank
    private String status; // Active, Inactive, Banned
}
