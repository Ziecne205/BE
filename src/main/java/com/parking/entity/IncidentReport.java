package com.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private ParkingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReportedByUserID", nullable = false)
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
    private User handledByStaff;

    @Column(name = "ResolvedAt")
    private LocalDateTime resolvedAt;

    @Column(name = "ResolutionNotes")
    private String resolutionNotes;
}
