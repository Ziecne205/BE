package com.parking.modules.manager;

import com.parking.entity.SystemConfig;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FeeConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public FeeConfigResponse getFeeConfig() {
        return FeeConfigResponse.builder()
                .hourlyRate(getBigDecimalValue("HOURLY_RATE", BigDecimal.ZERO))
                .depositPercent(getBigDecimalValue("DEPOSIT_PERCENT", BigDecimal.valueOf(0.50)))
                .overstayRatePerHour(getBigDecimalValue("OVERSTAY_RATE_PER_HOUR", BigDecimal.valueOf(50000))) // Default fallback
                // Gia tri gia han co dinh (legacy); tu Phase 3, no-show scheduler dung Reservation.checkinDeadline
                // (grace ti le theo do dai booking + depositPercentAtBooking, xem ReservationService.computeGracePeriod)
                // thay vi key nay. Giu lai key/getter/setter de khong pha FE dang render form nay.
                .noShowGraceMinutes(getIntegerValue("NO_SHOW_GRACE_MINUTES", 15))
                .blacklistThreshold(getIntegerValue("BLACKLIST_THRESHOLD", 3))
                .depositPaymentWindowMinutes(getIntegerValue("DEPOSIT_PAYMENT_WINDOW_MINUTES", 15))
                .build();
    }

    public FeeConfigResponse updateFeeConfig(FeeConfigRequest request) {
        saveConfig("HOURLY_RATE", request.getHourlyRate() != null ? request.getHourlyRate().toString() : "0");
        // Cac field @NotNull -> chi luu khi khong null (phong truong hop validation bi bo qua).
        saveConfigIfPresent("DEPOSIT_PERCENT", request.getDepositPercent());
        saveConfigIfPresent("OVERSTAY_RATE_PER_HOUR", request.getOverstayRatePerHour());
        saveConfigIfPresent("NO_SHOW_GRACE_MINUTES", request.getNoShowGraceMinutes());
        saveConfigIfPresent("BLACKLIST_THRESHOLD", request.getBlacklistThreshold());
        // Truoc day getFeeConfig doc key nay nhung updateFeeConfig khong luu -> khong the chinh qua API.
        saveConfigIfPresent("DEPOSIT_PAYMENT_WINDOW_MINUTES", request.getDepositPaymentWindowMinutes());

        return getFeeConfig();
    }

    private BigDecimal getBigDecimalValue(String key, BigDecimal defaultValue) {
        return systemConfigRepository.findById(key)
                .map(config -> new BigDecimal(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private Integer getIntegerValue(String key, Integer defaultValue) {
        return systemConfigRepository.findById(key)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(defaultValue);
    }

    /** Chi luu khi value khac null (giu gia tri hien tai neu request bo trong field do). */
    private void saveConfigIfPresent(String key, Object value) {
        if (value != null) {
            saveConfig(key, value.toString());
        }
    }

    private void saveConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findById(key).orElse(
                SystemConfig.builder().configKey(key).build()
        );
        config.setConfigValue(value);
        config.setDescription("Managed by FeeConfig API");
        systemConfigRepository.save(config);
    }
}
