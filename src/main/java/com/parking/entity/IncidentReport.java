package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "IncidentReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IncidentID")
    private Long incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SessionID")
    @JsonIgnore
    private ParkingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReportedByUserID", nullable = false)
    @JsonIgnore
    private User reportedBy;

    @Column(name = "IssueType", nullable = false)
    private String issueType;

    @Column(name = "Description", nullable = false)
    private String description;

    @Column(name = "ProofImageURL")
    private String proofImageUrl;

    @Column(name = "Status")
    private String status; // Open, InProgress, Resolved

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HandledByStaffID")
    @JsonIgnore
    private User handledByStaff;

    @Column(name = "ResolvedAt")
    private LocalDateTime resolvedAt;

    @Column(name = "ResolutionNotes")
    private String resolutionNotes;

    // Các Getter này giúp Jackson tự động lấy ID ra JSON mà KHÔNG cần khởi tạo
    // proxy của Hibernate
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
