package com.parking.modules.driver;

import com.parking.entity.PricingPolicy;
import com.parking.entity.SystemConfig;
import com.parking.entity.VehicleType;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.PricingPolicyRepository;
import com.parking.repository.SystemConfigRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class ParkingInfoService {

    private final SystemConfigRepository systemConfigRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public ParkingInfoResponse getParkingInfo() {
        // 1. Get Operating Hours
        String startHour = systemConfigRepository.findById("DAY_START_HOUR")
                .map(SystemConfig::getConfigValue)
                .orElse("06:00");
        String endHour = systemConfigRepository.findById("DAY_END_HOUR")
                .map(SystemConfig::getConfigValue)
                .orElse("18:00");
        String operatingHours = startHour + " - " + endHour;

        // 2. Get Pricing Policies (Active)
        List<PricingPolicy> activePolicies = pricingPolicyRepository.findAll().stream()
                .filter(p -> "Active".equals(p.getStatus()))
                .collect(Collectors.toList());

        List<PricingInfoDTO> pricingInfoList = activePolicies.stream().map(p -> PricingInfoDTO.builder()
                .vehicleTypeName(p.getVehicleType().getTypeName())
                .basePrice(p.getBasePrice())
                .baseHours(p.getBaseHours())
                .extraHourPrice(p.getExtraHourPrice())
                .nightSurcharge(p.getNightSurcharge())
                .build()).collect(Collectors.toList());

        // 3. Get Availability By Vehicle Type
        List<VehicleType> vehicleTypes = vehicleTypeRepository.findAll();
        List<SlotAvailabilityDTO> availabilityList = new ArrayList<>();
        long totalAvailableSlots = 0;

        for (VehicleType vt : vehicleTypes) {
            long total = parkingSlotRepository.countByVehicleType_VehicleTypeId(vt.getVehicleTypeId());
            long available = parkingSlotRepository.countByVehicleType_VehicleTypeIdAndStatus(vt.getVehicleTypeId(), "Available");
            totalAvailableSlots += available;

            availabilityList.add(SlotAvailabilityDTO.builder()
                    .vehicleTypeName(vt.getTypeName())
                    .totalSlots(total)
                    .availableSlots(available)
                    .build());
        }

        return ParkingInfoResponse.builder()
                .parkingName("Smart Parking System")
                .operatingHours(operatingHours)
                .totalAvailableSlots(totalAvailableSlots)
                .availabilityByVehicleType(availabilityList)
                .pricingPolicies(pricingInfoList)
                .build();
    }
}
