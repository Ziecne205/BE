package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "Reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    private UUID reservationId;

    @JsonIgnore
    @DBRef
    private User user;

    @JsonIgnore
    @DBRef
    private VehicleType vehicleType;

    private String licensePlate;

    private LocalDateTime expectedEntryTime;

    private LocalDateTime expectedExitTime;

    private BigDecimal depositAmount;

    private String depositStatus; // Pending, Paid, Forfeited, Refunded

    private String status; // Pending, Confirmed, CheckedIn, Fulfilled, Cancelled, Expired

    private LocalDateTime createdAt;

    // Phơi ID/tên phẳng ra JSON (user & vehicleType bị @JsonIgnore để khỏi lộ
    // passwordHash). FE đọc trực tiếp các field này.
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
