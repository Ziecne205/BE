package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.VehicleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/vehicle-types")
@RequiredArgsConstructor
@Tag(name = "Manager - Vehicle Types", description = "Quản lý danh mục loại phương tiện (Phân hệ 1)")
@PreAuthorize("hasRole('MANAGER')")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả loại xe")
    public ApiResponse<List<VehicleType>> findAll() {
        return ApiResponse.ok("Danh sách loại xe", vehicleTypeService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin một loại xe theo ID")
    public ApiResponse<VehicleType> findById(@PathVariable Integer id) {
        return ApiResponse.ok("Chi tiết loại xe", vehicleTypeService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo mới loại xe")
    public ApiResponse<VehicleType> create(@Valid @RequestBody VehicleTypeRequest request) {
        return ApiResponse.ok("Tạo loại xe thành công", vehicleTypeService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin loại xe")
    public ApiResponse<VehicleType> update(@PathVariable Integer id,
                                           @Valid @RequestBody VehicleTypeRequest request) {
        return ApiResponse.ok("Cập nhật loại xe thành công", vehicleTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá loại xe")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        vehicleTypeService.delete(id);
        return ApiResponse.ok("Xoá loại xe thành công", null);
    }
}
