package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ParkingSlots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SlotID")
    private Long slotId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "FloorID", nullable = false)
    private Floor floor;

    @Column(name = "Zone")
    private String zone;

    @Column(name = "SlotCode", nullable = false, unique = true)
    private String slotCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "VehicleTypeID", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "Status")
    private String status; // Available, Occupied, Maintenance — chỉ camera CV hoặc Manager đổi
}
