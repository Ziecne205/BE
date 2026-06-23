package com.parking.modules.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO bao cao doanh thu theo ngay/thang/nam.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class RevenueReportResponse {
    private LocalDate fromDate;
    private LocalDate toDate;
    /** Tong so phien gui xe hoan thanh */
    private long completedSessions;
    /** Tong doanh thu tu Payment.Amount */
    private BigDecimal totalRevenue;
    /** Doanh thu trung binh moi phien */
    private BigDecimal avgRevenuePerSession;
}
