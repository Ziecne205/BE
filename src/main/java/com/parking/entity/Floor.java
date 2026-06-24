package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Floors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FloorID")
    private Integer floorId;

    @Column(name = "FloorName", nullable = false, unique = true)
    private String floorName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "DedicatedVehicleTypeID")
    private VehicleType dedicatedVehicleType;

    @Column(name = "TotalCapacity", nullable = false)
    private Integer totalCapacity;
}
