package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import com.parking.entity.IncidentReport;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/incidents")
@RequiredArgsConstructor
@Tag(name = "Staff - Incidents", description = "Bien ban su co (CRUD)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ApiResponse<IncidentReport> create(@Valid @RequestBody IncidentRequest request, Authentication auth) {
        return ApiResponse.ok("Tao bien ban thanh cong", incidentService.create(request, auth.getName()));
    }

    @GetMapping
    public ApiResponse<List<IncidentReport>> findAll() {
        return ApiResponse.ok(incidentService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentReport> findById(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.findById(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<IncidentReport> resolve(@PathVariable Long id, @RequestBody String resolutionNotes, Authentication auth) {
        return ApiResponse.ok("Da xu ly su co", incidentService.resolve(id, resolutionNotes, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        incidentService.delete(id);
        return ApiResponse.ok("Da xoa bien ban", null);
    }
}
