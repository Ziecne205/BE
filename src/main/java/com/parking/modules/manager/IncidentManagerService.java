package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.IncidentReport;
import com.parking.entity.User;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quan ly Su co - Phan he 1 Manager.
 * Manager co the:
 * - Xem tat ca su co (loc theo status, issueType)
 * - Xu ly (chuyen trang thai InProgress / Resolved)
 * - Ghi chu bien ban giai quyet
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IncidentManagerService {

    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public List<IncidentReport> findAll() {
        return incidentRepository.findAll();
    }

    public List<IncidentReport> findByStatus(String status) {
        return incidentRepository.findByStatus(status);
    }

    public List<IncidentReport> findByIssueType(String issueType) {
        return incidentRepository.findByIssueType(issueType);
    }

    public IncidentReport findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay su co #" + id));
    }

    /**
     * Manager nhan xu ly su co: chuyen sang InProgress. Dung MongoTemplate.findAndModify
     * de dieu kien "status == Open" va cap nhat la mot thao tac nguyen tu duoi database
     * (tranh race khi 2 manager cung bam take-over cung luc — plain findById->mutate->save
     * se khien nguoi bam sau ghi de nguoi bam truoc ma khong bao loi).
     */
    @Transactional
    public IncidentReport takeOver(Long id, String managerUsername) {
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));

        Query query = Query.query(Criteria.where("_id").is(id).and("status").is("Open"));
        Update update = new Update()
                .set("status", "InProgress")
                .set("handledByStaff", manager)
                .set("takenOverAt", LocalDateTime.now());
        IncidentReport result = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), IncidentReport.class);
        if (result == null) {
            IncidentReport current = findById(id);
            throw new BusinessRuleException("Su co da duoc xu ly boi " + current.getHandledByStaffId(),
                    "INCIDENT_ALREADY_TAKEN");
        }
        return result;
    }

    /**
     * Manager giai quyet xong su co: chuyen sang Resolved, ghi bien ban.
     */
    @Transactional
    public IncidentReport resolve(Long id, String resolutionNotes, String managerUsername) {
        IncidentReport incident = findById(id);
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        incident.setStatus("Resolved");
        incident.setResolutionNotes(resolutionNotes);
        incident.setHandledByStaff(manager);
        incident.setResolvedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }
}
