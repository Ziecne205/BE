package com.parking.modules.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckOutRequest {

    @NotBlank(message = "Bien so khong duoc trong")
    private String licensePlate;

    @NotNull(message = "Cong ra khong duoc trong")
    private Integer exitGateId;

    private String exitImageUrl;

    private String paymentMethod; // Cash, QR, Card...

    /** Danh dau xe bi mat ve de tinh phu phi LostTicketFee. */
    private boolean lostTicket;

    /** So tien mat Staff thuc thu tai cong. Bo trong -> khong doi chieu, thu dung so he thong tinh. */
    private BigDecimal collectedAmount;

    /** Bat buoc neu collectedAmount lech qua CASH_TOLERANCE_VND so voi so he thong tinh. */
    private String discountReason;
}
