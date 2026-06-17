package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import com.parking.entity.ParkingSession;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/sessions")
@RequiredArgsConstructor
@Tag(name = "Staff - Sessions", description = "Check-in / Check-out tai barie (phan he Nhan vien)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/check-in")
    public ApiResponse<ParkingSession> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok("Xe vao thanh cong", sessionService.checkIn(request));
    }

    @PostMapping("/check-out")
    public ApiResponse<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok("Xe ra thanh cong", sessionService.checkOut(request));
    }
}
