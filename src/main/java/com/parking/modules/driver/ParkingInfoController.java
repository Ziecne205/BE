package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver/parking-info")
@RequiredArgsConstructor
@Tag(name = "Driver - Parking Info", description = "Public API to view parking lot status and pricing")
public class ParkingInfoController {

    private final ParkingInfoService parkingInfoService;

    @GetMapping
    @Operation(summary = "Get parking lot information", description = "Retrieves operating hours, active pricing policies, and current slot availability by vehicle type. Does not require authentication.")
    public ResponseEntity<ApiResponse<ParkingInfoResponse>> getParkingInfo() {
        ParkingInfoResponse info = parkingInfoService.getParkingInfo();
        return ResponseEntity.ok(ApiResponse.success(info, "Successfully retrieved parking info"));
    }
}
