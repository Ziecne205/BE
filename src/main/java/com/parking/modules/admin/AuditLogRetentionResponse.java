package com.parking.modules.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Tinh trang luu tru nhat ky: giu bao nhieu ngay, dang co bao nhieu dong, bao nhieu dong qua han. */
@Getter
@Builder
public class AuditLogRetentionResponse {

    /** So ngay giu log (SystemConfig AUDIT_LOG_RETENTION_DAYS); 0 = giu vinh vien. */
    private final int retentionDays;

    /** Moc thoi gian: log cu hon moc nay se bi don. Null khi giu vinh vien. */
    private final LocalDateTime cutoff;

    private final long totalLogs;

    /** So log da qua han, se bi xoa o lan don ke tiep. */
    private final long expiredLogs;
}
