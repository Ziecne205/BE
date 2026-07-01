package com.parking.modules.admin;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tong quan he thong cho Admin (1 toa, tong hop theo tang). Khop shape AdminDashboard ben FE.
 */
public record AdminDashboardResponse(
        List<FloorSummary> floors,
        Totals totals,
        List<UsagePoint> usageCurve) {

    public record FloorSummary(
            String floorId,
            String floorName,
            long capacity,
            long inside,
            long outstanding,
            long walkInHeadroom,
            double occupancyRate,
            long openIncidents,
            BigDecimal revenueToday) {
    }

    public record Totals(
            long capacity,
            long inside,
            long outstanding,
            long walkInHeadroom,
            double occupancyRate,
            long openIncidents,
            BigDecimal revenueToday) {
    }

    public record UsagePoint(String hour, double occupancyRate) {
    }
}
