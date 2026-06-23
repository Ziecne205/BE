package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/driver/sessions")
@RequiredArgsConstructor
@Tag(name = "Driver - Sessions", description = "Quan ly phien gui xe - phan he Khach hang")
@PreAuthorize("hasRole('DRIVER')")
public class SessionDriverController {

    private final SessionDriverService sessionDriverService;

    @GetMapping("/current")
    public ApiResponse<List<ParkingSessionDTO>> getCurrentSessions(Authentication auth) {
        return ApiResponse.ok(sessionDriverService.getCurrentSessions(auth.getName()));
    }

    @GetMapping("/history")
    public ApiResponse<List<ParkingSessionDTO>> getHistorySessions(Authentication auth) {
        return ApiResponse.ok(sessionDriverService.getHistorySessions(auth.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDetailResponse> getSessionDetail(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(sessionDriverService.getSessionDetail(id, auth.getName()));
    }
}
