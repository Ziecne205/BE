package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Nationalized;

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

    @Nationalized
    @Column(name = "TypeName", nullable = false, unique = true)
    private String typeName;

    @Nationalized
    @Column(name = "Dimensions")
    private String dimensions;
}
