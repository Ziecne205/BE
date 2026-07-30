package com.parking.scheduler;

import com.parking.modules.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Don nhat ky he thong qua han moi dem (03:15 — ngoai gio cao diem cua bai xe).
 *
 * <p>Khong dung TTL index cua MongoDB vi thoi gian giu phai chinh duoc luc dang chay qua
 * SystemConfig {@code AUDIT_LOG_RETENTION_DAYS}; TTL index doi thi phai drop/tao lai index.
 * Job chay moi ngay mot lan la du: log chi phinh dan theo ngay, khong can don theo phut.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogRetentionScheduler {

    private final AuditLogService auditLogService;

    @Scheduled(cron = "0 15 3 * * *")
    public void purgeExpiredAuditLogs() {
        try {
            long deleted = auditLogService.purgeExpiredLogs();
            log.info("Don nhat ky he thong: da xoa {} dong qua han", deleted);
        } catch (Exception e) {
            // Job nen: loi o day khong duoc lam chet lich chay cac dem sau.
            log.error("Don nhat ky he thong that bai: {}", e.getMessage(), e);
        }
    }
}
