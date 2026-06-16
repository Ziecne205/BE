package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidentRequest {

    private Long sessionId;

    @NotBlank
    private String issueType;

    @NotBlank
    private String description;

    private String proofImageUrl;
}
