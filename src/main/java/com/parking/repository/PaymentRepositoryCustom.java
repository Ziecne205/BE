package com.parking.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepositoryCustom {
    /** Tong doanh thu (PaymentStatus = 'Success') trong khoang thoi gian. */
    BigDecimal sumRevenueByPeriod(LocalDateTime from, LocalDateTime to);
}
