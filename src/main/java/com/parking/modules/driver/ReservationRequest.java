package com.parking.modules.driver;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationRequest {

    @NotNull
    private Integer vehicleTypeId;

    @NotBlank(message = "Bien so la bat buoc khi dat cho (muc 8.2)")
    private String licensePlate;

    @NotNull
    @Future
    private LocalDateTime expectedEntryTime;

    @NotNull
    @Future
    private LocalDateTime expectedExitTime;
}
