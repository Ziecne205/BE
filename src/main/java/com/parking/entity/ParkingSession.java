package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ParkingSessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReservationID")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CardID")
    private ParkingCard card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DriverUserID")
    private User driver;

    @Column(name = "LicensePlateIn", nullable = false)
    private String licensePlateIn;

    @Column(name = "LicensePlateOut")
    private String licensePlateOut;

    @Column(name = "EntryImageURL")
    private String entryImageUrl;

    @Column(name = "ExitImageURL")
    private String exitImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleTypeID", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "EntryTime", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "ExitTime")
    private LocalDateTime exitTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EntryGateID", nullable = false)
    private Gate entryGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ExitGateID")
    private Gate exitGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SuggestedSlotID")
    private ParkingSlot suggestedSlot;

    @Column(name = "SuggestedSlotHoldExpiresAt")
    private LocalDateTime suggestedSlotHoldExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ActualSlotID")
    private ParkingSlot actualSlot;

    @Column(name = "Status")
    private String status; // Admitted, Parked, Moved, Completed, Exception
}
