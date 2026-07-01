package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mo phong Cong & Camera cho Staff (demo). Path /api/gate/* va /api/camera/*.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Staff - Simulation", description = "Mo phong cong vao/ra va camera CV (demo)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/gate/entry/scan")
    @Operation(summary = "Mo phong quet bien so cong vao (co ty le loi)")
    public ApiResponse<SimulationDtos.EntryScanResult> entryScan(@RequestBody SimulationDtos.ScanRequest req) {
        double fr = req.failureRate() == null ? 0 : req.failureRate();
        return ApiResponse.ok("Ket qua quet cong vao", simulationService.entryScan(req.licensePlate(), fr));
    }

    @PostMapping("/gate/exit/scan")
    @Operation(summary = "Mo phong quet bien so cong ra (xem phi)")
    public ApiResponse<SimulationDtos.ExitScanResult> exitScan(@RequestBody SimulationDtos.ScanRequest req) {
        double fr = req.failureRate() == null ? 0 : req.failureRate();
        return ApiResponse.ok("Ket qua quet cong ra", simulationService.exitScan(req.licensePlate(), fr));
    }

    @PostMapping("/gate/force-checkin")
    @Operation(summary = "Cho vao thu cong (bo qua kiem tra suc chua)")
    public ApiResponse<SimulationDtos.ForceCheckinResult> forceCheckin(@RequestBody SimulationDtos.PlateRequest req) {
        return ApiResponse.ok("Da cho vao thu cong", simulationService.forceCheckin(req.licensePlate()));
    }

    @PostMapping("/camera/slot-occupied")
    @Operation(summary = "Camera bao o co xe (Occupied)")
    public ApiResponse<SimulationDtos.CameraSlotResult> slotOccupied(
            @RequestBody SimulationDtos.CameraOccupiedRequest req) {
        return ApiResponse.ok("Da cap nhat o", simulationService.cameraSlotOccupied(req.slotCode(), req.licensePlate()));
    }

    @PostMapping("/camera/slot-vacated")
    @Operation(summary = "Camera bao o trong (Available)")
    public ApiResponse<SimulationDtos.CameraSlotResult> slotVacated(
            @RequestBody SimulationDtos.CameraVacatedRequest req) {
        return ApiResponse.ok("Da cap nhat o", simulationService.cameraSlotVacated(req.slotCode()));
    }
}
