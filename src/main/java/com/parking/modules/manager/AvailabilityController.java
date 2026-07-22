package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/availability")
@RequiredArgsConstructor
@Tag(name = "Manager - Availability", description = "Cho trong real-time theo loai xe (headroom + zone)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "Headroom vang lai + cho trong theo loai xe va khu vuc")
    public ApiResponse<AvailabilityResponse> getAvailability() {
        return ApiResponse.ok("Tinh trang cho trong", availabilityService.getAvailability());
    }
}
