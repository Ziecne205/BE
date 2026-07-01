package com.parking.modules.manager;

import java.math.BigDecimal;

/** Cac diem chuoi thoi gian cho bao cao (khop RevenuePoint / OccupancyWindow ben FE). */
public class ReportSeries {

    public record RevenuePoint(String date, BigDecimal revenue, long sessions, double occupancyRate) {
    }

    public record OccupancyWindow(String windowStart, String windowEnd, long entries, long exits, long inside) {
    }

    private ReportSeries() {
    }
}
