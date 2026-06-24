package com.parking.modules.admin;

import com.parking.common.ApiResponse;
import com.parking.entity.SystemConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequiredArgsConstructor
@Tag(name = "Admin - System Config", description = "Cau hinh tham so he thong (phan he Quan tri vien)")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(summary = "Xem toan bo cau hinh he thong")
    public ApiResponse<List<SystemConfig>> findAll() {
        return ApiResponse.ok(systemConfigService.findAll());
    }

    @GetMapping("/{key}")
    @Operation(summary = "Xem chi tiet mot cau hinh")
    public ApiResponse<SystemConfig> findByKey(@PathVariable String key) {
        return ApiResponse.ok(systemConfigService.findByKey(key));
    }

    @PostMapping
    @Operation(summary = "Tao cau hinh moi")
    public ApiResponse<SystemConfig> create(@Valid @RequestBody SystemConfigRequest request) {
        return ApiResponse.ok("Tao cau hinh thanh cong", systemConfigService.create(request));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Cap nhat gia tri cau hinh")
    public ApiResponse<SystemConfig> update(@PathVariable String key, @Valid @RequestBody SystemConfigRequest request) {
        return ApiResponse.ok("Cap nhat thanh cong", systemConfigService.update(key, request));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Xoa cau hinh")
    public ApiResponse<Void> delete(@PathVariable String key) {
        systemConfigService.delete(key);
        return ApiResponse.ok("Da xoa cau hinh", null);
    }
}
