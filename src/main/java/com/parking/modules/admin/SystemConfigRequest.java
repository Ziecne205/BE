package com.parking.modules.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigRequest {

    // Bat buoc khi tao moi (POST) — validate trong SystemConfigService.create.
    // KHONG @NotBlank vi khi update (PUT) key lay tu path, body khong can gui lai configKey.
    private String configKey;

    @NotBlank(message = "Config value khong duoc trong")
    private String configValue;

    private String description;
}
