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
                .depositPercent(getBigDecimalValue("DEPOSIT_PERCENT", BigDecimal.valueOf(0.20)))
                .overstayRatePerHour(getBigDecimalValue("OVERSTAY_RATE_PER_HOUR", BigDecimal.valueOf(50000))) // Default fallback
                .noShowGraceMinutes(getIntegerValue("NO_SHOW_GRACE_MINUTES", 15))
                .blacklistThreshold(getIntegerValue("BLACKLIST_THRESHOLD", 3))
                .build();
    }

    public FeeConfigResponse updateFeeConfig(FeeConfigRequest request) {
        saveConfig("HOURLY_RATE", request.getHourlyRate() != null ? request.getHourlyRate().toString() : "0");
        saveConfig("DEPOSIT_PERCENT", request.getDepositPercent().toString());
        saveConfig("OVERSTAY_RATE_PER_HOUR", request.getOverstayRatePerHour().toString());
        saveConfig("NO_SHOW_GRACE_MINUTES", request.getNoShowGraceMinutes().toString());
        saveConfig("BLACKLIST_THRESHOLD", request.getBlacklistThreshold().toString());

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

    private void saveConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findById(key).orElse(
                SystemConfig.builder().configKey(key).build()
        );
        config.setConfigValue(value);
        config.setDescription("Managed by FeeConfig API");
        systemConfigRepository.save(config);
    }
}
