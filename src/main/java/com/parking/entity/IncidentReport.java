package com.parking.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "IncidentReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {

    @Id
    private Long incidentId;

    @DBRef
    @JsonIgnore
    private ParkingSession session;

    @DBRef
    @JsonIgnore
    private User reportedBy;

    private String issueType;

    private String description;

    private String proofImageUrl;

    private String status; // Open, InProgress, Resolved

    @DBRef
    @JsonIgnore
    private User handledByStaff;

    private LocalDateTime resolvedAt;

    private String resolutionNotes;

    private LocalDateTime createdAt;

    /** Moc thoi gian Staff nhan xu ly su co — co so cho job escalate su co bi ket (Phase 6). */
    private LocalDateTime takenOverAt;

    /** Da bi escalate roi hay chua — tranh job escalate chay lai nhieu lan tren cung 1 su co. */
    private LocalDateTime escalatedAt;

    /** Optimistic locking cho thao tac take-over nguyen tu (Phase 6). */
    @Version
    private Long version;

    // Các Getter này phơi ID phẳng ra JSON (ref bị @JsonIgnore).
    public Long getSessionId() {
        return session != null ? session.getSessionId() : null;
    }

    public Long getReportedByUserId() {
        return reportedBy != null ? reportedBy.getUserId() : null;
    }

    public Long getHandledByStaffId() {
        return handledByStaff != null ? handledByStaff.getUserId() : null;
    }
}
