package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingInfoResponse {
    private String parkingName;
    private String operatingHours;
    private long totalAvailableSlots;
    private List<SlotAvailabilityDTO> availabilityByVehicleType;
    private List<PricingInfoDTO> pricingPolicies;
}
