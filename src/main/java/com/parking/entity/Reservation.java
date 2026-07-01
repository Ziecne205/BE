package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID")
    private Long reservationId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleTypeID", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "LicensePlate", nullable = false)
    private String licensePlate;

    @Column(name = "ExpectedEntryTime", nullable = false)
    private LocalDateTime expectedEntryTime;

    @Column(name = "ExpectedExitTime", nullable = false)
    private LocalDateTime expectedExitTime;

    @Column(name = "DepositAmount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "DepositStatus")
    private String depositStatus; // Pending, Paid, Forfeited, Refunded

    @Column(name = "Status")
    private String status; // Pending, Confirmed, CheckedIn, Fulfilled, Cancelled, Expired

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    // Phơi ID/tên phẳng ra JSON (user & vehicleType bị @JsonIgnore để khỏi lộ
    // passwordHash / proxy Hibernate). FE đọc trực tiếp các field này.
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Integer getVehicleTypeId() {
        return vehicleType != null ? vehicleType.getVehicleTypeId() : null;
    }

    public String getVehicleTypeName() {
        return vehicleType != null ? vehicleType.getTypeName() : null;
    }
}
