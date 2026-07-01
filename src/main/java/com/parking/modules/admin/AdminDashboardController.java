package com.parking.modules.admin;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Tong quan he thong (KPI, trang thai tang, duong cong su dung)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    @Operation(summary = "Tong quan toan he thong cho Admin")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.ok("Tong quan he thong", adminDashboardService.getDashboard());
    }
}
