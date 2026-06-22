package com.parking.modules.admin;

import com.parking.common.ApiResponse;
import com.parking.entity.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs", description = "Nhat ky he thong (phan he Quan tri vien)")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Xem toan bo nhat ky he thong")
    public ApiResponse<List<AuditLog>> findAll() {
        return ApiResponse.ok(auditLogService.findAll());
    }

    @GetMapping("/by-action")
    @Operation(summary = "Loc nhat ky theo hanh dong (VD: STAFF_CHECK_IN)")
    public ApiResponse<List<AuditLog>> findByAction(@RequestParam String action) {
        return ApiResponse.ok(auditLogService.findByAction(action));
    }

    @GetMapping("/by-entity")
    @Operation(summary = "Loc nhat ky theo doi tuong (VD: ParkingSession)")
    public ApiResponse<List<AuditLog>> findByEntity(@RequestParam String entityName) {
        return ApiResponse.ok(auditLogService.findByEntityName(entityName));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Loc nhat ky theo nguoi thuc hien")
    public ApiResponse<List<AuditLog>> findByUser(@PathVariable Long userId) {
        return ApiResponse.ok(auditLogService.findByUserId(userId));
    }

    @GetMapping("/by-date")
    @Operation(summary = "Loc nhat ky theo khoang thoi gian")
    public ApiResponse<List<AuditLog>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(auditLogService.findByDateRange(from, to));
    }
}
