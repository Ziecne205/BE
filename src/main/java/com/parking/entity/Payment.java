package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID", nullable = false)
    private ParkingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReservationID")
    private Reservation reservation;

    @Column(name = "Amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "PaymentMethod", nullable = false)
    private String paymentMethod;

    @Column(name = "PaymentTime", nullable = false)
    private LocalDateTime paymentTime;

    @Column(name = "PaymentStatus")
    private String paymentStatus; // Success, Failed, Pending

    @Column(name = "TransactionReference")
    private String transactionReference;
}
