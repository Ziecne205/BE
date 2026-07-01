package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotAvailabilityDTO {
    private Integer vehicleTypeId;
    private String vehicleTypeName;
    private long totalSlots;
    private long availableSlots;
}
