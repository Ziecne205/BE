package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Dung khi bien so quet duoc tai cong khong khop voi bien so cua booking/phien hien tai.
 * Staff xac nhan cho vao thu cong voi bien so thuc te da quet.
 */
@Getter
@Setter
public class ForceCheckInRequest {

    @NotBlank(message = "Bien so thuc te khong duoc de trong")
    private String actualPlate;

    private String reason;
}
