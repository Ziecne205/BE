package com.parking.modules.manager;

import com.parking.entity.ParkingSession;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ParkingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bao cao & Thong ke - Phan he 1 Manager.
 * - Bao cao doanh thu theo ngay/thang/nam
 * - Bao cao luu luong xe ra/vao
 * - Phan tich khung gio cao diem va ty le lap day
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;

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
}
