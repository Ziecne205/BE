package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PricingPolicies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID")
    private Integer policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleTypeID", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "BasePrice", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "BaseHours", nullable = false)
    private Integer baseHours;

    @Column(name = "ExtraHourPrice", nullable = false)
    private BigDecimal extraHourPrice;

    @Column(name = "NightSurcharge")
    private BigDecimal nightSurcharge;

    @Column(name = "LostTicketFee")
    private BigDecimal lostTicketFee;

    @Column(name = "EffectiveDate", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "Status")
    private String status; // Active, Expired
}
