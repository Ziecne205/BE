package com.parking.modules.staff;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.IncidentReport;
import com.parking.entity.ParkingSession;
import com.parking.entity.User;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class IncidentService {

    private final IncidentReportRepository incidentRepository;
    private final ParkingSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public IncidentReport create(IncidentRequest request, String reporterUsername) {
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));

        ParkingSession session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien gui xe"));
        }

        IncidentReport incident = IncidentReport.builder()
                .session(session)
                .reportedBy(reporter)
                .issueType(request.getIssueType())
                .description(request.getDescription())
                .proofImageUrl(request.getProofImageUrl())
                .status("Open")
                .createdAt(LocalDateTime.now())
                .build();
        return incidentRepository.save(incident);
    }

    public List<IncidentReport> findAll() {
        return incidentRepository.findAll();
    }

    public IncidentReport findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay su co #" + id));
    }

    public IncidentReport resolve(Long id, String resolutionNotes, String handlerUsername) {
        IncidentReport incident = findById(id);
        User handler = userRepository.findByUsername(handlerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));

        incident.setStatus("Resolved");
        incident.setResolutionNotes(resolutionNotes);
        incident.setHandledByStaff(handler);
        incident.setResolvedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    public void delete(Long id) {
        if (!incidentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Khong tim thay su co #" + id);
        }
        incidentRepository.deleteById(id);
    }
}
