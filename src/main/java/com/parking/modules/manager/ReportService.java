package com.parking.modules.manager;

import com.parking.entity.ParkingSession;
import com.parking.entity.Payment;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Bao cao & Thong ke - Phan he 1 Manager.
 * - Bao cao doanh thu theo ngay/thang/nam
 * - Bao cao luu luong xe ra/vao
 * - Phan tich khung gio cao diem va ty le lap day
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class ReportService {

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingSlotRepository slotRepository;

    private static final List<String> ACTIVE_STATUSES = List.of("Admitted", "Parked", "Moved");

    /**
     * Bao cao doanh thu trong khoang fromDate -> toDate.
     */
    public RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        BigDecimal totalRevenue = paymentRepository.sumRevenueByPeriod(from, to);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        List<ParkingSession> completedSessions = sessionRepository
                .findByStatusAndEntryTimeBetween("Completed", from, to);

        long count = completedSessions.size();
        BigDecimal avg = count > 0
                ? totalRevenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RevenueReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .completedSessions(count)
                .totalRevenue(totalRevenue)
                .avgRevenuePerSession(avg)
                .build();
    }

    /**
     * Bao cao luu luong xe va phan tich khung gio cao diem.
     */
    public TrafficReportResponse getTrafficReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<ParkingSession> allSessions = sessionRepository.findByEntryTimeBetween(from, to);

        long totalEntries = allSessions.size();
        long totalExits = allSessions.stream()
                .filter(s -> "Completed".equals(s.getStatus())).count();
        long currentInside = sessionRepository.countByStatusIn(ACTIVE_STATUSES);
        long withReservation = allSessions.stream()
                .filter(s -> s.getReservation() != null).count();
        long walkIn = totalEntries - withReservation;

        // Phan tich gio cao diem dua tren EntryTime
        Map<Integer, Long> byHour = allSessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEntryTime().getHour(),
                        Collectors.counting()
                ));

        int peakHour = byHour.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(-1);
        long peakHourCount = peakHour >= 0 ? byHour.getOrDefault(peakHour, 0L) : 0;

        return TrafficReportResponse.builder()
                .period(fromDate + " -> " + toDate)
                .totalEntries(totalEntries)
                .totalExits(totalExits)
                .currentInside(currentInside)
                .walkInCount(walkIn)
                .reservationCount(withReservation)
                .peakHour(peakHour)
                .peakHourCount(peakHourCount)
                .build();
    }

    /**
     * Doanh thu theo tung ngay trong khoang (chuoi thoi gian cho bieu do).
     * occupancyRate la uoc luong tho: so phien/ngay tren suc chua hien tai.
     */
    public List<ReportSeries.RevenuePoint> getRevenueDaily(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        long capacity = slotRepository.count() - slotRepository.countByStatus("Maintenance");

        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        Map<LocalDate, Long> sessionsByDay = new TreeMap<>();
        for (Payment p : paymentRepository.findByPaymentTimeBetween(from, to)) {
            if (!"Success".equals(p.getPaymentStatus()) || p.getPaymentTime() == null) continue;
            LocalDate day = p.getPaymentTime().toLocalDate();
            revenueByDay.merge(day, p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(), BigDecimal::add);
            sessionsByDay.merge(day, 1L, Long::sum);
        }

        List<ReportSeries.RevenuePoint> points = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            long sessions = sessionsByDay.getOrDefault(d, 0L);
            BigDecimal revenue = revenueByDay.getOrDefault(d, BigDecimal.ZERO);
            double occ = capacity > 0 ? Math.min(100.0, Math.round((double) sessions / capacity * 1000.0) / 10.0) : 0;
            points.add(new ReportSeries.RevenuePoint(d.toString(), revenue, sessions, occ));
        }
        return points;
    }

    /**
     * Phan bo luu luong theo khung 2 gio (profile mot ngay dien hinh gom ca khoang).
     * inside = cong don (entries - exits) qua cac khung.
     */
    public List<ReportSeries.OccupancyWindow> getOccupancyHourly(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<ParkingSession> sessions = sessionRepository.findByEntryTimeBetween(from, to);

        long[] entries = new long[12]; // 12 khung 2 gio: 0-2, 2-4, ... 22-24
        long[] exits = new long[12];
        for (ParkingSession s : sessions) {
            entries[s.getEntryTime().getHour() / 2]++;
            if (s.getExitTime() != null) {
                exits[s.getExitTime().getHour() / 2]++;
            }
        }

        List<ReportSeries.OccupancyWindow> windows = new ArrayList<>();
        long inside = 0;
        for (int i = 0; i < 12; i++) {
            inside = Math.max(0, inside + entries[i] - exits[i]);
            windows.add(new ReportSeries.OccupancyWindow(
                    String.format("%02d:00", i * 2),
                    String.format("%02d:00", (i * 2 + 2) % 24),
                    entries[i], exits[i], inside));
        }
        return windows;
    }
}
