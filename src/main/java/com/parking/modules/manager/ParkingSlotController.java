package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.ParkingSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/slots")
@RequiredArgsConstructor
@Tag(name = "Manager - Parking Slots", description = "Quan ly o do xe - co so ha tang bai do (Phan he 1)")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    @GetMapping
    @Operation(summary = "Lay danh sach tat ca o do xe")
    public ApiResponse<List<ParkingSlot>> findAll() {
        return ApiResponse.ok("Danh sach o do xe", parkingSlotService.findAll());
    }

    @GetMapping("/floor/{floorId}")
    @Operation(summary = "Lay danh sach o do xe theo tang")
    public ApiResponse<List<ParkingSlot>> findByFloor(@PathVariable Integer floorId) {
        return ApiResponse.ok("Danh sach o do xe tang " + floorId, parkingSlotService.findByFloor(floorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lay thong tin mot o do xe theo ID")
    public ApiResponse<ParkingSlot> findById(@PathVariable Long id) {
        return ApiResponse.ok("Chi tiet o do xe", parkingSlotService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tao moi o do xe")
    public ApiResponse<ParkingSlot> create(@Valid @RequestBody ParkingSlotRequest request) {
        return ApiResponse.ok("Tao o do xe thanh cong", parkingSlotService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cap nhat thong tin o do xe")
    public ApiResponse<ParkingSlot> update(@PathVariable Long id,
                                           @Valid @RequestBody ParkingSlotRequest request) {
        return ApiResponse.ok("Cap nhat o do xe thanh cong", parkingSlotService.update(id, request));
    }

    @PatchMapping("/{id}/maintenance")
    @Operation(summary = "Chuyen o sang trang thai Maintenance (chi Manager)")
    public ApiResponse<ParkingSlot> setMaintenance(@PathVariable Long id,
                                                   @RequestParam boolean maintenance) {
        return ApiResponse.ok(
                maintenance ? "Da chuyen o sang Maintenance" : "Da tra o ve Available",
                parkingSlotService.setMaintenance(id, maintenance));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoa o do xe (khong co xe)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        parkingSlotService.delete(id);
        return ApiResponse.ok("Xoa o do xe thanh cong", null);
    }
}
