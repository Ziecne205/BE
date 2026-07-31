package com.parking.modules.manualrefund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ManualRefundSubmitRequest {

    @NotNull(message = "reservationId is required")
    private UUID reservationId;

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "bankInfo is required")
    private String bankInfo;
}
