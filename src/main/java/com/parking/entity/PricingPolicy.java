package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "PricingPolicies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPolicy {

    @Id
    private Integer policyId;

    @DBRef
    private VehicleType vehicleType;

    private BigDecimal basePrice;

    private Integer baseHours;

    private BigDecimal extraHourPrice;

    private BigDecimal nightSurcharge;

    private BigDecimal lostTicketFee;

    private LocalDateTime effectiveDate;

    private String status; // Active, Expired
}
