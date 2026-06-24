package com.parking.modules.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigRequest {

    @NotBlank(message = "Config key khong duoc trong")
    private String configKey;

    @NotBlank(message = "Config value khong duoc trong")
    private String configValue;

    private String description;
}
