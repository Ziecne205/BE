package com.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "BookingQuotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuotaID")
    private Integer quotaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleTypeID", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private VehicleType vehicleType;

    @Column(name = "StartTime", nullable = false, columnDefinition = "TIME")
    private LocalTime startTime;

    @Column(name = "EndTime", nullable = false, columnDefinition = "TIME")
    private LocalTime endTime;

    @Column(name = "QuotaPercent", nullable = false)
    private BigDecimal quotaPercent; // % of C, theo loai xe, khong theo tang

    /** Bat/tat hieu luc quota. Quota tat -> khong ap tran (khong chan dat cho). */
    @Column(name = "IsActive")
    @Builder.Default
    private Boolean isActive = true;
}
