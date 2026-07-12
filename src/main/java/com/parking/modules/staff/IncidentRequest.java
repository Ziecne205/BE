package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidentRequest {

    private Long sessionId;

    @NotBlank
    @Pattern(
        regexp = "Other|CameraMiss|Overstay|CapacityCrash|PlateMismatch|ExitTailgating|Loiterer|LostCard",
        message = "Loai su co khong hop le (Other, CameraMiss, Overstay, CapacityCrash, PlateMismatch, ExitTailgating, Loiterer, LostCard)")
    private String issueType;

    @NotBlank
    private String description;

    private String proofImageUrl;
}
