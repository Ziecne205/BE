package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/dashboard")
@RequiredArgsConstructor
@Tag(name = "Manager - Dashboard", description = "Giam sat trang thai bai do xe real-time (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Tong quan toan bai do: so o trong/co xe/bao tri, phien mo, su co chua xu ly")
    public ApiResponse<ParkingOverviewResponse> getOverview() {
        return ApiResponse.ok("Tong quan bai do xe", dashboardService.getOverview());
    }

    @GetMapping("/floors")
    @Operation(summary = "Trang thai tung tang: so o theo trang thai, ty le lap day")
    public ApiResponse<List<FloorStatusResponse>> getFloorStatus() {
        return ApiResponse.ok("Trang thai theo tung tang", dashboardService.getFloorStatus());
    }
}
