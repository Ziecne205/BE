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
}
