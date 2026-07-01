package com.parking.modules.admin;

import com.parking.common.ApiResponse;
import com.parking.entity.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Quan ly tai khoan (phan he Quan tri vien)")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping
    public ApiResponse<User> createUser(@Valid @RequestBody AdminUserCreationRequest request) {
        return ApiResponse.ok("Tao nguoi dung thanh cong", userAdminService.createUser(request));
    }

    @GetMapping
    public ApiResponse<List<User>> findAll() {
        return ApiResponse.ok(userAdminService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> findById(@PathVariable Long id) {
        return ApiResponse.ok(userAdminService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<User> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok("Cap nhat trang thai thanh cong", userAdminService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        userAdminService.resetPassword(id, newPassword);
        return ApiResponse.ok("Da reset mat khau", null);
    }
}
