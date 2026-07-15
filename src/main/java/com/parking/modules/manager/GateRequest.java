package com.parking.modules.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GateRequest {

    @NotBlank
    private String gateName;

    /** Entity khong co enum rieng, chi validate o day. */
    @NotBlank
    @Pattern(regexp = "Entry|Exit", message = "gateType chi nhan 'Entry' hoac 'Exit'")
    private String gateType;

    @NotNull
    private Integer floorId;
}
