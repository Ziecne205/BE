package com.parking.modules.driver;

import com.parking.entity.Reservation;
import com.parking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private static final BigDecimal DEPOSIT_PERCENT = BigDecimal.valueOf(0.20);

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void expireUnpaidReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(3);
        List<Reservation> pendingReservations = reservationRepository.findByStatusAndCreatedAtBefore("Pending", cutoff);
        for (Reservation reservation : pendingReservations) {
            reservation.setStatus("Cancelled");
            // Optional: call PayOS verification here to be absolutely sure it wasn't paid recently
            reservationRepository.save(reservation);
            log.info("Cancelled unpaid reservation: {}", reservation.getReservationId());
        }
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void expireNoShowReservations() {
        List<Reservation> confirmedReservations = reservationRepository.findByStatus("Confirmed");
        LocalDateTime now = LocalDateTime.now();

        for (Reservation reservation : confirmedReservations) {
            LocalDateTime entry = reservation.getExpectedEntryTime();
            LocalDateTime exit = reservation.getExpectedExitTime();
            long durationMinutes = Duration.between(entry, exit).toMinutes();

            // Tính toán thời gian ân hạn thô (graceRaw) = duration * phần trăm cọc
            long graceRaw = (long) (durationMinutes * DEPOSIT_PERCENT.doubleValue());

            // Mức trần (cap)
            long capMinutes;
            if (durationMinutes < 24 * 60) {
                capMinutes = 2 * 60; // 2 giờ
            } else if (durationMinutes <= 7 * 24 * 60) {
                capMinutes = 12 * 60; // 12 giờ
            } else {
                capMinutes = 24 * 60; // 24 giờ
            }

            // Mức sàn (15 phút) và giới hạn
            long gracePeriod = Math.max(15, Math.min(graceRaw, capMinutes));
            LocalDateTime checkInDeadline = entry.plusMinutes(gracePeriod);

            if (now.isAfter(checkInDeadline)) {
                reservation.setStatus("Expired");
                reservation.setDepositStatus("Forfeited");
                reservationRepository.save(reservation);
                log.info("Expired no-show reservation: {} (Grace period was {} mins)", reservation.getReservationId(), gracePeriod);
            }
        }
    }
}
