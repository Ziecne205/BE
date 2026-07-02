package com.parking.modules.admin;

import com.parking.entity.ParkingSession;
import com.parking.modules.manager.DashboardService;
import com.parking.modules.manager.FloorStatusResponse;
import com.parking.modules.manager.ParkingOverviewResponse;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tong quan toan he thong cho Admin: KPI tong, trang thai tung tang, duong cong
 * su dung theo gio.
 * Tai chinh/su co khong gan theo tang (schema khong ho tro) -> de o phan
 * Totals.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final DashboardService dashboardService;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ParkingSessionRepository sessionRepository;

    private static final List<String> INSIDE_STATUSES = List.of("Admitted", "Parked", "Moved");
    private static final List<String> OUTSTANDING_STATUSES = List.of("Confirmed");

    public AdminDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        ParkingOverviewResponse overview = dashboardService.getOverview();
        List<FloorStatusResponse> floorStatuses = dashboardService.getFloorStatus();

        List<AdminDashboardResponse.FloorSummary> floors = new ArrayList<>();
        for (FloorStatusResponse f : floorStatuses) {
            long usable = f.getAvailableSlots() + f.getOccupiedSlots();
            floors.add(new AdminDashboardResponse.FloorSummary(
                    String.valueOf(f.getFloorId()),
                    f.getFloorName(),
                    usable,
                    f.getOccupiedSlots(),
                    0, // outstanding khong gan theo tang
                    f.getAvailableSlots(),
                    f.getOccupancyRate(),
                    0, // su co khong gan theo tang
                    BigDecimal.ZERO));
        }

        long outstanding = reservationRepository
                .countByStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
                        OUTSTANDING_STATUSES, now, now);
        BigDecimal revenueToday = paymentRepository.sumRevenueByPeriod(startOfDay, now);
        if (revenueToday == null)
            revenueToday = BigDecimal.ZERO;

        long usableTotal = overview.getAvailableSlots() + overview.getOccupiedSlots();
        AdminDashboardResponse.Totals totals = new AdminDashboardResponse.Totals(
                usableTotal,
                overview.getOccupiedSlots(),
                outstanding,
                overview.getAvailableSlots(),
                overview.getOverallOccupancyRate(),
                overview.getPendingIncidentsCount(),
                revenueToday);

        return new AdminDashboardResponse(floors, totals, buildUsageCurve(startOfDay, now, usableTotal));
    }

    /**
     * Ty le lap day tai moc 2h trong ngay hom nay (dua tren phien vao/ra thuc te).
     */
    private List<AdminDashboardResponse.UsagePoint> buildUsageCurve(
            LocalDateTime startOfDay, LocalDateTime now, long capacity) {
        // Gop phien dang mo + phien vao trong hom nay (dedup theo id).
        Map<Long, ParkingSession> sessions = new LinkedHashMap<>();
        for (ParkingSession s : sessionRepository.findByStatusIn(INSIDE_STATUSES)) {
            sessions.put(s.getSessionId(), s);
        }
        for (ParkingSession s : sessionRepository.findByEntryTimeBetween(startOfDay, now)) {
            sessions.put(s.getSessionId(), s);
        }

        List<AdminDashboardResponse.UsagePoint> points = new ArrayList<>();
        for (int h = 0; h <= 24; h += 2) {
            LocalDateTime t = startOfDay.plusHours(h);
            long inside = sessions.values().stream()
                    .filter(s -> !s.getEntryTime().isAfter(t))
                    .filter(s -> s.getExitTime() == null || s.getExitTime().isAfter(t))
                    .count();
            double rate = capacity > 0 ? Math.round((double) inside / capacity * 1000.0) / 10.0 : 0;
            points.add(new AdminDashboardResponse.UsagePoint(String.format("%02d:00", h), rate));
        }
        return points;
    }
}
