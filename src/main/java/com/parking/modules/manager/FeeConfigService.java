package com.parking.modules.manager;

import com.parking.entity.SystemConfig;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FeeConfigService {

    private final SystemConfigRepository systemConfigRepository;

    private static final String HOURLY_RATE = "HOURLY_RATE";
    private static final String DEPOSIT_PERCENT = "DEPOSIT_PERCENT";
    private static final String OVERSTAY_RATE_PER_HOUR = "OVERSTAY_RATE_PER_HOUR";
    private static final String NO_SHOW_GRACE_MINUTES = "NO_SHOW_GRACE_MINUTES";
    private static final String BLACKLIST_THRESHOLD = "BLACKLIST_THRESHOLD";

    public FeeConfigDTO getFeeConfig() {
        return FeeConfigDTO.builder()
                .hourlyRate(getBigDecimal(HOURLY_RATE, BigDecimal.valueOf(10000)))
                .depositPercent(getBigDecimal(DEPOSIT_PERCENT, BigDecimal.valueOf(20)))
                .overstayRatePerHour(getBigDecimal(OVERSTAY_RATE_PER_HOUR, BigDecimal.valueOf(10000)))
                .noShowGraceMinutes(getInteger(NO_SHOW_GRACE_MINUTES, 30))
                .blacklistThreshold(getInteger(BLACKLIST_THRESHOLD, 3))
                .build();
    }

    @Transactional
    public FeeConfigDTO updateFeeConfig(FeeConfigDTO request) {
        saveConfig(HOURLY_RATE, request.getHourlyRate().toString(), "Flat hourly rate");
        saveConfig(DEPOSIT_PERCENT, request.getDepositPercent().toString(), "Deposit percentage for booking");
        saveConfig(OVERSTAY_RATE_PER_HOUR, request.getOverstayRatePerHour().toString(), "Overstay penalty per hour");
        saveConfig(NO_SHOW_GRACE_MINUTES, request.getNoShowGraceMinutes().toString(), "Grace minutes before no-show");
        saveConfig(BLACKLIST_THRESHOLD, request.getBlacklistThreshold().toString(), "Threshold for blacklist");
        return getFeeConfig();
    }

    private void saveConfig(String key, String value, String desc) {
        SystemConfig config = systemConfigRepository.findById(key).orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(desc);
        systemConfigRepository.save(config);
    }

    private BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return systemConfigRepository.findById(key)
                .map(config -> new BigDecimal(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private Integer getInteger(String key, Integer defaultValue) {
        return systemConfigRepository.findById(key)
                .map(config -> Integer.valueOf(config.getConfigValue()))
                .orElse(defaultValue);
    }
}
