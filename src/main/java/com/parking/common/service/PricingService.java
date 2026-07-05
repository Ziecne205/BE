package com.parking.common.service;

import com.parking.entity.PricingPolicy;
import com.parking.repository.PricingPolicyRepository;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PricingService {

    private final PricingPolicyRepository pricingPolicyRepository;
    private final SystemConfigRepository systemConfigRepository;

    /**
     * Phi co ban theo thoi luong: gia goc + (so gio vuot baseHours) * gia moi gio vuot.
     * Day la cong thuc base+extra DUY NHAT — ca uoc tinh phi ben Driver (calculateFee) va
     * tinh phi checkout ben Staff (SessionService) deu goi ham nay de tranh logic trong lech nhau.
     */
    public BigDecimal baseAndExtra(PricingPolicy policy, long minutes) {
        long totalHours = (long) Math.ceil(minutes / 60.0);
        BigDecimal fee = policy.getBasePrice();
        long extraHours = totalHours - policy.getBaseHours();
        if (extraHours > 0) {
            fee = fee.add(policy.getExtraHourPrice().multiply(BigDecimal.valueOf(extraHours)));
        }
        return fee;
    }

    public BigDecimal calculateFee(Integer vehicleTypeId, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (exitTime == null) {
            exitTime = LocalDateTime.now();
        }

        PricingPolicy policy = pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(vehicleTypeId, "Active")
                .orElseThrow(() -> new RuntimeException("No active pricing policy found for vehicle type"));

        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes <= 0) return BigDecimal.ZERO;

        BigDecimal fee = baseAndExtra(policy, minutes);

        // Read day-window config — tolerates both "6" and "06:00" storage formats
        int dayStartHour = parseHourConfig(
                systemConfigRepository.findById("DAY_START_HOUR")
                        .map(c -> c.getConfigValue()).orElse(null),
                6);
        int dayEndHour = parseHourConfig(
                systemConfigRepository.findById("DAY_END_HOUR")
                        .map(c -> c.getConfigValue()).orElse(null),
                18);

        if (policy.getNightSurcharge() != null
                && checkNightOverlap(entryTime, exitTime, dayStartHour, dayEndHour)) {
            fee = fee.add(policy.getNightSurcharge());
        }

        return fee;
    }

    /**
     * Returns true when the parking window [start, end) overlaps any nighttime hour,
     * i.e. any time outside the half-open day window [dayStartHour, dayEndHour).
     *
     * Uses direct time-boundary comparison instead of an hourly loop so that:
     *   - Performance is O(1) regardless of duration.
     *   - A session ending exactly at dayEndHour (e.g. 18:00) is NOT charged a
     *     night surcharge — the exclusive upper bound is intentional.
     */
    private boolean checkNightOverlap(LocalDateTime start, LocalDateTime end,
                                       int dayStartHour, int dayEndHour) {
        // Multi-day stay always crosses a night window
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return true;
        }
        // Same calendar day — check whether any part falls outside [dayStart, dayEnd)
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime   = end.toLocalTime();
        LocalTime dayStart  = LocalTime.of(dayStartHour, 0);
        LocalTime dayEnd    = LocalTime.of(dayEndHour, 0);

        // Overlap with night exists if the session starts before day-start OR ends after day-end
        return startTime.isBefore(dayStart) || endTime.isAfter(dayEnd);
    }

    /**
     * Parses an hour integer from a SystemConfig value.
     * Accepts both bare integers ("6", "18") and "HH:mm" strings ("06:00", "18:00").
     * Falls back to {@code defaultVal} on null input or parse failure.
     */
    private int parseHourConfig(String value, int defaultVal) {
        if (value == null || value.isBlank()) return defaultVal;
        String trimmed = value.contains(":") ? value.split(":")[0].trim() : value.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
