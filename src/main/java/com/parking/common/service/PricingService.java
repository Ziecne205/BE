package com.parking.common.service;

import com.parking.entity.PricingPolicy;
import com.parking.entity.SystemConfig;
import com.parking.repository.PricingPolicyRepository;
import com.parking.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PricingService {

    @Autowired
    private PricingPolicyRepository pricingPolicyRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    public BigDecimal calculateFee(Integer vehicleTypeId, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (exitTime == null) {
            exitTime = LocalDateTime.now();
        }

        PricingPolicy policy = pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(vehicleTypeId, "Active")
                .orElseThrow(() -> new RuntimeException("No active pricing policy found for vehicle type"));

        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes <= 0)
            return BigDecimal.ZERO;

        long totalHours = (long) Math.ceil((double) minutes / 60);

        BigDecimal fee = policy.getBasePrice();
        long extraHours = totalHours - policy.getBaseHours();

        if (extraHours > 0) {
            fee = fee.add(policy.getExtraHourPrice().multiply(BigDecimal.valueOf(extraHours)));
        }

        // Night surcharge check (simplistic overlap check)
        int dayStartHour = 6;
        int dayEndHour = 18;

        SystemConfig dayStartConfig = systemConfigRepository.findById("DAY_START_HOUR").orElse(null);
        SystemConfig dayEndConfig = systemConfigRepository.findById("DAY_END_HOUR").orElse(null);

        if (dayStartConfig != null)
            dayStartHour = Integer.parseInt(dayStartConfig.getConfigValue());
        if (dayEndConfig != null)
            dayEndHour = Integer.parseInt(dayEndConfig.getConfigValue());

        boolean hasNightHours = checkNightOverlap(entryTime, exitTime, dayStartHour, dayEndHour);
        if (hasNightHours && policy.getNightSurcharge() != null) {
            fee = fee.add(policy.getNightSurcharge());
        }

        return fee;
    }

    private boolean checkNightOverlap(LocalDateTime start, LocalDateTime end, int dayStartHour, int dayEndHour) {
        LocalDateTime temp = start;
        while (temp.isBefore(end)) {
            int hour = temp.getHour();
            if (hour < dayStartHour || hour >= dayEndHour) {
                return true;
            }
            temp = temp.plusHours(1);
        }
        return false;
    }
}
