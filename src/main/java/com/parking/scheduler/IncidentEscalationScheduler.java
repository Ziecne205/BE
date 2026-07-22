package com.parking.scheduler;

import com.parking.common.service.AuditLogWriter;
import com.parking.entity.IncidentReport;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Su co "InProgress" da duoc Staff/Manager nhan xu ly (takenOverAt) nhung qua lau van chua
 * Resolved -> nghi van bi ket (nguoi xu ly quen/roi vi tri) -> tu dong mo lai de nguoi khac
 * co the nhan. Khong co ha tang push/email notification (chi co EmailService cho OTP) nen
 * "thong bao cho manager khac" duoc thuc hien bang cach dua incident tro lai status "Open"
 * (da hien thi ngay trong danh sach incident cua Manager) + ghi AuditLog de truy vet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentEscalationScheduler {

    private static final int DEFAULT_STUCK_TIMEOUT_MINUTES = 60;
    private static final String STUCK_TIMEOUT_CONFIG_KEY = "INCIDENT_STUCK_TIMEOUT_MINUTES";

    private final IncidentReportRepository incidentReportRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogWriter auditLogWriter;

    /**
     * Moi 10 phut: incident "InProgress" (chua Resolved) ma takenOverAt (hoac lan escalate
     * truoc, neu co) da qua INCIDENT_STUCK_TIMEOUT_MINUTES -> mo lai "Open", xoa nguoi xu ly
     * cu de nguoi khac co the take-over, va ghi AuditLog "INCIDENT_AUTO_ESCALATED".
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void escalateStuckIncidents() {
        int timeoutMinutes = getStuckTimeoutMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<IncidentReport> stuck = incidentReportRepository
                .findByStatusAndResolvedAtIsNullAndTakenOverAtBefore("InProgress", threshold);
        for (IncidentReport incident : stuck) {
            // Da escalate roi ma van chua Resolved trong 1 timeout nua -> escalate lai; neu chua
            // tung escalate thi luon xu ly (escalatedAt == null).
            if (incident.getEscalatedAt() != null && incident.getEscalatedAt().isAfter(threshold)) {
                continue;
            }
            Long previousHandler = incident.getHandledByStaffId();
            incident.setStatus("Open");
            incident.setHandledByStaff(null);
            incident.setEscalatedAt(LocalDateTime.now());
            incidentReportRepository.save(incident);

            auditLogWriter.log(null, "INCIDENT_AUTO_ESCALATED", "IncidentReport",
                    String.valueOf(incident.getIncidentId()),
                    "Su co #" + incident.getIncidentId() + " bi ket qua " + timeoutMinutes
                            + " phut (nguoi xu ly truoc: " + previousHandler + ") - tu dong mo lai de nguoi khac nhan");
            log.warn("Incident {} auto-escalated back to Open after {} minutes stuck InProgress (previous handler: {})",
                    incident.getIncidentId(), timeoutMinutes, previousHandler);
        }
    }

    private int getStuckTimeoutMinutes() {
        return systemConfigRepository.findById(STUCK_TIMEOUT_CONFIG_KEY)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(DEFAULT_STUCK_TIMEOUT_MINUTES);
    }
}
