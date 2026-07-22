package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ParkingSessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSession {

    @Id
    private Long sessionId;

    @DBRef
    private Reservation reservation;

    @DBRef
    private ParkingCard card;

    @DBRef
    private User driver;

    private String licensePlateIn;

    private String licensePlateOut;

    private String entryImageUrl;

    private String exitImageUrl;

    @DBRef
    private VehicleType vehicleType;

    private LocalDateTime entryTime;

    private LocalDateTime exitTime;

    @DBRef
    private Gate entryGate;

    @DBRef
    private Gate exitGate;

    @DBRef
    private ParkingSlot suggestedSlot;

    private LocalDateTime suggestedSlotHoldExpiresAt;

    @DBRef
    private ParkingSlot actualSlot;

    private String status; // Admitted, Parked, Moved, Completed, Exception

    /** Staff force-checked-in vehicle (plate mismatch vs reservation, overridden manually). */
    @Builder.Default
    private Boolean isForceCheckIn = false;

    /** Session exceeded the pricing policy grace period at checkout (overstay surcharge applied). */
    @Builder.Default
    private Boolean isOverstay = false;

    /** Session currently past expectedExitTime + 30min while still open (set by Phase 4's job) — distinct from isOverstay, which is only set at checkOut as a historical marker. */
    @Builder.Default
    private Boolean isOverstayFlagged = false;
}
