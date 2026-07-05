package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Floors")
// Floor xuat hien long nhau trong ParkingSlot.floor; sau merge co the la Hibernate proxy.
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
