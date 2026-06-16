package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "VehicleTypes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleTypeID")
    private Integer vehicleTypeId;

    @Column(name = "TypeName", nullable = false, unique = true)
    private String typeName;

    @Column(name = "Dimensions")
    private String dimensions;
}
