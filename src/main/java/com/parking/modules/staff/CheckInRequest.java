package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckInRequest {

    @NotBlank(message = "Bien so khong duoc trong")
    private String licensePlate;

    @NotNull(message = "Loai xe khong duoc trong")
    private Integer vehicleTypeId;

    @NotNull(message = "Cong vao khong duoc trong")
    private Integer entryGateId;

    private String entryImageUrl;

    private UUID reservationId;

    /** Neu khach da dang nhap app va check-in walk-in (khong dat cho truoc). */
    private Long driverUserId;
}
