package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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

    /** Truyen ReservationID neu xe nay co dat cho truoc (luong 6.D). */
    private Long reservationId;
}
