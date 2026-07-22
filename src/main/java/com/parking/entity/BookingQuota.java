package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalTime;

@Document(collection = "BookingQuotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingQuota {

    @Id
    private Integer quotaId;

    @DBRef
    private VehicleType vehicleType;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal quotaPercent; // % of C, theo loai xe, khong theo tang

    /** Bat/tat hieu luc quota. Quota tat -> khong ap tran (khong chan dat cho). */
    @Builder.Default
    private Boolean isActive = true;
}
