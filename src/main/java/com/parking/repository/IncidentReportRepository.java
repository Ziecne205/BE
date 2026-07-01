package com.parking.repository;

import com.parking.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    java.util.List<IncidentReport> findByStatus(String status);
    java.util.List<IncidentReport> findByIssueType(String issueType);
    long countByStatus(String status);

    /** Tranh scheduler tao trung su co cho cung 1 phien moi lan chay lai (vd: moi 5 phut). */
    boolean existsBySession_SessionIdAndIssueType(Long sessionId, String issueType);
}
