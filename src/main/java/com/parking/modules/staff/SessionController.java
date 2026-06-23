package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/sessions")
@RequiredArgsConstructor
@Tag(name = "Staff - Sessions", description = "Check-in / Check-out tai barie (phan he Nhan vien)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/check-in")
    @Operation(summary = "Check-in xe vao bai")
    public ApiResponse<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok("Xe vao thanh cong", sessionService.checkIn(request));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Check-out xe ra khoi bai")
    public ApiResponse<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ApiResponse.ok("Xe ra thanh cong", sessionService.checkOut(request));
    }

    @GetMapping("/active")
    @Operation(summary = "Danh sach phien dang mo (Admitted/Parked)")
    public ApiResponse<List<ActiveSessionDto>> getActiveSessions() {
        return ApiResponse.ok("Danh sach phien dang mo", sessionService.getActiveSessions());
    }

    @GetMapping("/search")
    @Operation(summary = "Tim phien dang mo theo bien so (ho tro check-out)")
    public ApiResponse<ActiveSessionDto> searchByPlate(@RequestParam String licensePlate) {
        return ApiResponse.ok("Tim thay phien", sessionService.searchActiveByPlate(licensePlate));
    }
}
