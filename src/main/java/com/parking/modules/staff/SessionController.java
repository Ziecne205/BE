package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import com.parking.modules.driver.PayosLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ApiResponse<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request, Authentication auth) {
        CheckOutResponse response = sessionService.checkOut(request, auth.getName());
        return ApiResponse.ok(response.isPendingApproval()
                ? "So tien thu lech qua muc cho phep - da gui Manager duyet"
                : "Xe ra thanh cong", response);
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

    @PostMapping("/{id}/force-check-in")
    @Operation(summary = "Cho vao thu cong khi bien so quet duoc khong khop voi booking (danh dau isForceCheckIn + ghi audit log)")
    public ApiResponse<CheckInResponse> forceCheckIn(@PathVariable Long id,
                                                      @Valid @RequestBody ForceCheckInRequest request) {
        return ApiResponse.ok("Da force check-in", sessionService.forceCheckIn(id, request));
    }

    @PostMapping("/{id}/payos-link")
    @Operation(summary = "Tao QR PayOS cho phi gui xe cua phien (dynamic theo thoi gian do) - cong ra")
    public ApiResponse<PayosLinkResponse> createFeeLink(@PathVariable Long id) {
        return ApiResponse.ok("Tao QR thanh toan thanh cong", sessionService.createFeeLink(id));
    }
}
