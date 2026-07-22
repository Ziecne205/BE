package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.IncidentReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/incidents")
@RequiredArgsConstructor
@Tag(name = "Manager - Incidents", description = "Quan ly su co bai do xe - xem, xu ly, giai quyet (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class IncidentManagerController {

    private final IncidentManagerService incidentManagerService;

    @GetMapping
    @Operation(summary = "Lay tat ca su co (co the loc theo status)")
    public ApiResponse<List<IncidentReport>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String issueType) {
        if (status != null) {
            return ApiResponse.ok("Su co theo trang thai", incidentManagerService.findByStatus(status));
        }
        if (issueType != null) {
            return ApiResponse.ok("Su co theo loai", incidentManagerService.findByIssueType(issueType));
        }
        return ApiResponse.ok("Tat ca su co", incidentManagerService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet mot su co")
    public ApiResponse<IncidentReport> findById(@PathVariable Long id) {
        return ApiResponse.ok("Chi tiet su co", incidentManagerService.findById(id));
    }

    @PatchMapping("/{id}/take-over")
    @Operation(summary = "Manager nhan xu ly su co (InProgress)")
    public ApiResponse<IncidentReport> takeOver(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Da nhan xu ly su co", incidentManagerService.takeOver(id, auth.getName()));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Manager giai quyet xong su co, ghi bien ban")
    public ApiResponse<IncidentReport> resolve(@PathVariable Long id,
                                               @RequestParam String resolutionNotes,
                                               Authentication auth) {
        return ApiResponse.ok("Da giai quyet su co", incidentManagerService.resolve(id, resolutionNotes, auth.getName()));
    }
}
