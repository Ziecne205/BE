package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "Payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private Long paymentId;

    @DBRef
    @JsonIgnore
    private ParkingSession session;

    @DBRef
    @JsonIgnore
    private Reservation reservation;

    private BigDecimal amount;

    private String paymentMethod;

    private LocalDateTime paymentTime;

    private String paymentStatus; // Success, Failed, Pending

    private String transactionReference;

    // Phan biet giao dich nay la coc/phi checkout/gia han/hoan tien — can thiet tu khi phi gia
    // han (Phase 5) co the ton tai song song voi giao dich coc/phi goc tren cung 1 booking/phien.
    private String paymentPurpose; // Deposit, Fee, Extension, Refund

    // Trang thai hoan tien PayOS (Phase 6) — null nghia la chua yeu cau hoan tien.
    private String refundStatus; // null, Requested, AutoRefunded, ManualRequired, Failed

    private LocalDateTime refundedAt;
}
