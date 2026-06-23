package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DedicatedVehicleTypeID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private VehicleType dedicatedVehicleType;

    @Column(name = "TotalCapacity", nullable = false)
    private Integer totalCapacity;
}
