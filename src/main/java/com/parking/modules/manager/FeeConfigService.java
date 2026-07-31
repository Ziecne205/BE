package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.entity.SystemConfig;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeConfigService {

    private static final List<String> ACTIVE_SESSION_STATUSES = List.of("Admitted", "Parked", "Moved");

    private final SystemConfigRepository systemConfigRepository;
    private final ParkingSessionRepository parkingSessionRepository;

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
                .depositPaymentWindowMinutes(getIntegerValue("DEPOSIT_PAYMENT_WINDOW_MINUTES", 3))
                .cancelWindowMinutes(getIntegerValue("CANCEL_WINDOW_MINUTES", 10))
                .cashToleranceVnd(getBigDecimalValue("CASH_TOLERANCE_VND", BigDecimal.ZERO))
                .build();
    }

    public FeeConfigResponse updateFeeConfig(FeeConfigRequest request) {
        if ((request.getDepositPercent() != null || request.getOverstayRatePerHour() != null)
                && parkingSessionRepository.countByStatusIn(ACTIVE_SESSION_STATUSES) > 0) {
            throw new BusinessRuleException(
                    "Khong the doi deposit%/overstay rate khi con phien do xe dang hoat dong",
                    "ACTIVE_SESSIONS_EXIST");
        }
        saveConfig("HOURLY_RATE", request.getHourlyRate() != null ? request.getHourlyRate().toString() : "0");
        // Cac field @NotNull -> chi luu khi khong null (phong truong hop validation bi bo qua).
        saveConfigIfPresent("DEPOSIT_PERCENT", request.getDepositPercent());
        saveConfigIfPresent("OVERSTAY_RATE_PER_HOUR", request.getOverstayRatePerHour());
        saveConfigIfPresent("NO_SHOW_GRACE_MINUTES", request.getNoShowGraceMinutes());
        saveConfigIfPresent("BLACKLIST_THRESHOLD", request.getBlacklistThreshold());
        // Truoc day getFeeConfig doc key nay nhung updateFeeConfig khong luu -> khong the chinh qua API.
        saveConfigIfPresent("DEPOSIT_PAYMENT_WINDOW_MINUTES", request.getDepositPaymentWindowMinutes());
        saveConfigIfPresent("CANCEL_WINDOW_MINUTES", request.getCancelWindowMinutes());
        saveConfigIfPresent("CASH_TOLERANCE_VND", request.getCashToleranceVnd());

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
