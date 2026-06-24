package com.parking.modules.admin;

import com.parking.common.ApiResponse;
import com.parking.entity.Permission;
import com.parking.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
@Tag(name = "Admin - RBAC", description = "Quan ly phan quyen Role-Permission (phan he Quan tri vien)")
@PreAuthorize("hasRole('ADMIN')")
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/roles")
    @Operation(summary = "Xem danh sach tat ca vai tro")
    public ApiResponse<List<Role>> findAllRoles() {
        return ApiResponse.ok(rbacService.findAllRoles());
    }

    @GetMapping("/roles/{roleId}")
    @Operation(summary = "Xem chi tiet vai tro")
    public ApiResponse<Role> findRoleById(@PathVariable Integer roleId) {
        return ApiResponse.ok(rbacService.findRoleById(roleId));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Xem danh sach tat ca quyen han")
    public ApiResponse<List<Permission>> findAllPermissions() {
        return ApiResponse.ok(rbacService.findAllPermissions());
    }

    @GetMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Xem danh sach quyen han cua mot vai tro")
    public ApiResponse<Set<Permission>> getPermissionsByRole(@PathVariable Integer roleId) {
        return ApiResponse.ok(rbacService.getPermissionsByRole(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Gan toan bo quyen han cho vai tro (ghi de)")
    public ApiResponse<Role> assignPermissions(@PathVariable Integer roleId,
                                                @Valid @RequestBody RolePermissionRequest request) {
        return ApiResponse.ok("Gan quyen thanh cong", rbacService.assignPermissions(roleId, request.getPermissionIds()));
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Them mot quyen han vao vai tro")
    public ApiResponse<Role> addPermission(@PathVariable Integer roleId, @PathVariable Integer permissionId) {
        return ApiResponse.ok("Them quyen thanh cong", rbacService.addPermission(roleId, permissionId));
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Xoa mot quyen han khoi vai tro")
    public ApiResponse<Role> removePermission(@PathVariable Integer roleId, @PathVariable Integer permissionId) {
        return ApiResponse.ok("Xoa quyen thanh cong", rbacService.removePermission(roleId, permissionId));
    }
}
