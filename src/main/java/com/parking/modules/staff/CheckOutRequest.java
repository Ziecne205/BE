package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckOutRequest {

    @NotBlank(message = "Bien so khong duoc trong")
    private String licensePlate;

    @NotNull(message = "Cong ra khong duoc trong")
    private Integer exitGateId;

    private String exitImageUrl;

    private String paymentMethod; // Cash, QR, Card...
}
