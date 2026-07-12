package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "VehicleTypes")
// Khi entity nay xuat hien long nhau (vd PricingPolicy.vehicleType) sau mot lan merge,
// Hibernate tra ve proxy — bo qua noi that cua proxy de Jackson serialize field that.
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
