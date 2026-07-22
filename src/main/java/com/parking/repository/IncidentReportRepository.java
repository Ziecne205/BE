package com.parking.repository;

import com.parking.entity.IncidentReport;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentReportRepository extends MongoRepository<IncidentReport, Long> {
    List<IncidentReport> findByStatus(String status);
    List<IncidentReport> findByIssueType(String issueType);
    long countByStatus(String status);

    /** Tranh scheduler tao trung su co cho cung 1 phien moi lan chay lai (vd: moi 5 phut). */
    @ExistsQuery("{ 'session.$id': ?0, 'issueType': ?1 }")
    boolean existsBySession_SessionIdAndIssueType(Long sessionId, String issueType);

    /** Su co con dang xu ly (chua Resolved) nhung da nhan take-over qua lau -> can escalate (Phase 6). */
    List<IncidentReport> findByStatusAndResolvedAtIsNullAndTakenOverAtBefore(String status, LocalDateTime threshold);
}
