package com.parking.modules.admin;

import com.parking.common.PageResponse;
import com.parking.entity.AuditLog;
import com.parking.repository.AuditLogRepository;
import com.parking.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    /** So ngay giu nhat ky, chinh duoc qua API SystemConfig. Dat 0 (hoac am) = giu vinh vien. */
    public static final String RETENTION_DAYS_KEY = "AUDIT_LOG_RETENTION_DAYS";

    /** ~6 thang: du de tra cuu/doi soat mot mua cao diem ma khong de bang log phinh vo han. */
    private static final int DEFAULT_RETENTION_DAYS = 180;

    /** Chan duoi de mot lan go nham config (vd "1") khong xoa sach lich su tra cuu. */
    private static final int MIN_RETENTION_DAYS = 30;

    private final AuditLogRepository auditLogRepository;
    private final SystemConfigRepository systemConfigRepository;

    /** Man "xem toan bo": phan trang, moi nhat truoc. size gioi han de tranh payload qua lon. */
    public PageResponse<AuditLog> findAll(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return PageResponse.of(auditLogRepository.findAll(pageable));
    }

    public List<AuditLog> findByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    public List<AuditLog> findByEntityName(String entityName) {
        return auditLogRepository.findByEntityName(entityName);
    }

    public List<AuditLog> findByUserId(Long userId) {
        return auditLogRepository.findByUser_UserId(userId);
    }

    public List<AuditLog> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        return auditLogRepository.findByCreatedAtBetween(from, to);
    }

    // ── Luu tru nhat ky (log retention) ──────────────────────────────────────────────────────
    // Bai xe ~1000 luot ra/vao moi ngay -> hang tram nghin dong log/nam, phan lon la log van hanh
    // het gia tri tra cuu tu lau. De nguyen thi vua ton dung luong vua lam cham moi truy van tren
    // collection nay. Giai phap: giu N ngay (cau hinh duoc), job nen don phan qua han moi dem.

    /** So ngay giu log theo cau hinh; <=0 nghia la giu vinh vien (tat tu dong don). */
    public int getRetentionDays() {
        int configured = systemConfigRepository.findById(RETENTION_DAYS_KEY)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue().trim());
                    } catch (NumberFormatException e) {
                        log.warn("{} = '{}' khong phai so, dung mac dinh {} ngay",
                                RETENTION_DAYS_KEY, config.getConfigValue(), DEFAULT_RETENTION_DAYS);
                        return DEFAULT_RETENTION_DAYS;
                    }
                })
                .orElse(DEFAULT_RETENTION_DAYS);

        if (configured <= 0) {
            return 0; // giu vinh vien
        }
        if (configured < MIN_RETENTION_DAYS) {
            log.warn("{} = {} ngay thap hon muc toi thieu, dung {} ngay",
                    RETENTION_DAYS_KEY, configured, MIN_RETENTION_DAYS);
            return MIN_RETENTION_DAYS;
        }
        return configured;
    }

    /** Moc don log, null khi giu vinh vien. */
    public LocalDateTime retentionCutoff() {
        int days = getRetentionDays();
        return days <= 0 ? null : LocalDateTime.now().minusDays(days);
    }

    /** Cho man Admin thay dang giu bao nhieu ngay / bao nhieu dong sap bi don. */
    public AuditLogRetentionResponse getRetentionStatus() {
        LocalDateTime cutoff = retentionCutoff();
        return AuditLogRetentionResponse.builder()
                .retentionDays(getRetentionDays())
                .cutoff(cutoff)
                .totalLogs(auditLogRepository.count())
                .expiredLogs(cutoff == null ? 0 : auditLogRepository.countByCreatedAtBefore(cutoff))
                .build();
    }

    /** Xoa log cu hon moc luu tru. Tra ve so dong da xoa (0 khi cau hinh giu vinh vien). */
    @Transactional
    public long purgeExpiredLogs() {
        LocalDateTime cutoff = retentionCutoff();
        if (cutoff == null) {
            log.debug("{} <= 0: giu nhat ky vinh vien, bo qua lan don nay", RETENTION_DAYS_KEY);
            return 0;
        }
        long deleted = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Da don {} dong nhat ky cu hon {} ({} ngay luu tru)", deleted, cutoff, getRetentionDays());
        }
        return deleted;
    }
}
