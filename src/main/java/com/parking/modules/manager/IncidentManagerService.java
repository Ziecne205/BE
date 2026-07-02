package com.parking.modules.manager;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.IncidentReport;
import com.parking.entity.User;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
     * Manager nhan xu ly su co: chuyen sang InProgress.
     */
    @Transactional
    public IncidentReport takeOver(Long id, String managerUsername) {
        IncidentReport incident = findById(id);
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        incident.setStatus("InProgress");
        incident.setHandledByStaff(manager);
        return incidentRepository.save(incident);
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
