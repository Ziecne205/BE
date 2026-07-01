package com.parking.scheduler;

import com.parking.entity.IncidentReport;
import com.parking.entity.ParkingSession;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.modules.driver.ReservationService;
import com.parking.modules.manager.FeeConfigService;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cac tac vu nen dinh ky de don dep phien/booking "treo" ma khong ai xu ly qua luong nghiep vu
 * binh thuong (staff/driver quen thao tac). Khong co API tuong ung — chi chay ngam theo cron.
 *
 * Assumption (do spec khong noi ro con so cu the):
 * - "Admitted" qua 15 phut khong tien trien -> nghi van bo xe/gian doan quy trinh check-in.
 *   IssueType dung "Loiterer" (gan nghia nhat voi "xe/nguoi la vang o cong qua lau" trong enum
 *   hien co: LostCard, Loiterer, ExitTailgating, PlateMismatch, CapacityCrash, Overstay, CameraMiss, Other).
 * - "Moved" qua 30 phut khong check-out -> tu dong dong phien (Completed) + tao IncidentReport
 *   voi IssueType "Overstay" (gan nghia nhat: xe da roi vi tri nhung khong hoan tat thu tuc ra).
 * - Reservation qua han (expectedExitTime) ma van con Pending/Confirmed (chua CheckedIn) ->
 *   No-show: chuyen "Expired" + mat coc (forfeit), tai su dung ReservationService.cancelWithRefund
 *   voi refund=false thay vi viet lai logic hoan/mat coc.
 * - Tan suat: 2 job dau moi 5 phut (kip thoi phat hien su co tai cong), job reservation moi 15
 *   phut (it khan cap hon, chi can don dep truoc ca lam viec tiep theo).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
@SuppressWarnings("null")
public class SessionExpiryScheduler {

    private static final int ADMITTED_STALE_MINUTES = 15;
    private static final int MOVED_STALE_MINUTES = 30;

    private static final String ISSUE_TYPE_LOITERER = "Loiterer";
    private static final String ISSUE_TYPE_OVERSTAY = "Overstay";

    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final FeeConfigService feeConfigService;

    /**
     * Moi 5 phut: phien "Admitted" qua 15 phut ma chua duoc ghi o thuc te (Parked) hay check-out
     * -> tao IncidentReport (Loiterer) de Manager/Staff kiem tra thuc te tai cong.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void flagStaleAdmittedSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ADMITTED_STALE_MINUTES);
        List<ParkingSession> staleSessions = sessionRepository.findByStatusAndEntryTimeBefore("Admitted", threshold);
        for (ParkingSession session : staleSessions) {
            if (incidentReportRepository.existsBySession_SessionIdAndIssueType(session.getSessionId(), ISSUE_TYPE_LOITERER)) {
                continue; // da bao roi, tranh spam incident moi 5 phut cho cung 1 phien
            }
            createIncident(session, ISSUE_TYPE_LOITERER,
                    "Phien Admitted #" + session.getSessionId() + " qua " + ADMITTED_STALE_MINUTES
                            + " phut khong tien trien (chua ghi o / chua check-out) - he thong tu dong bao");
            log.warn("Session {} flagged as stale Admitted (Loiterer)", session.getSessionId());
        }
    }

    /**
     * Moi 5 phut: phien "Moved" qua 30 phut khong check-out -> tu dong dong phien (Completed) va
     * tao IncidentReport (Overstay) de doi soat thu cong sau (khong the tinh phi chinh xac vi
     * khong co exitGate/thanh toan thuc te tai thoi diem tu dong dong).
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void autoCloseStaleMovedSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(MOVED_STALE_MINUTES);
        List<ParkingSession> staleSessions = sessionRepository.findByStatusAndEntryTimeBefore("Moved", threshold);
        for (ParkingSession session : staleSessions) {
            session.setStatus("Completed");
            session.setExitTime(LocalDateTime.now());
            sessionRepository.save(session);

            createIncident(session, ISSUE_TYPE_OVERSTAY,
                    "Phien Moved #" + session.getSessionId() + " qua " + MOVED_STALE_MINUTES
                            + " phut khong check-out - he thong tu dong dong phien va bao su co de doi soat");
            log.warn("Session {} auto-closed from stale Moved status", session.getSessionId());
        }
    }

    /**
     * Moi 15 phut: booking qua han (expectedExitTime) ma van chua duoc khach nhan xe
     * (status con Pending/Confirmed, chua CheckedIn) -> danh dau Expired + mat coc.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void expireNoShowReservations() {
        LocalDateTime now = LocalDateTime.now();
        int graceMinutes = feeConfigService.getFeeConfig().getNoShowGraceMinutes();
        LocalDateTime cutoffTime = now.minusMinutes(graceMinutes);

        List<Reservation> noShowReservations = reservationRepository
                .findByStatusInAndExpectedExitTimeBefore(List.of("Pending", "Confirmed"), cutoffTime);
        for (Reservation reservation : noShowReservations) {
            reservationService.cancelWithRefund(reservation, "Expired", false);
            log.warn("Reservation {} marked Expired (no-show) and deposit forfeited", reservation.getReservationId());
        }
    }

    private void createIncident(ParkingSession session, String issueType, String description) {
        User reporter = systemReporter();
        if (reporter == null) {
            log.error("Khong the tao IncidentReport tu scheduler: khong tim thay user ADMIN/MANAGER lam nguoi bao cao");
            return;
        }
        IncidentReport incident = IncidentReport.builder()
                .session(session)
                .reportedBy(reporter)
                .issueType(issueType)
                .description(description)
                .status("Open")
                .createdAt(LocalDateTime.now())
                .build();
        incidentReportRepository.save(incident);
    }

    /** IncidentReport.reportedBy la bat buoc (nullable=false) nen scheduler muon "nguoi bao cao" la mot ADMIN co san. */
    private User systemReporter() {
        List<User> admins = userRepository.findByRole_RoleNameOrderByUserIdAsc("ADMIN");
        if (!admins.isEmpty()) return admins.get(0);
        List<User> managers = userRepository.findByRole_RoleNameOrderByUserIdAsc("MANAGER");
        return managers.isEmpty() ? null : managers.get(0);
    }
}
