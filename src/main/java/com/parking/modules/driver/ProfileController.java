package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/profile")
@RequiredArgsConstructor
@Tag(name = "Driver - Profile", description = "Ho so tai khoan cua nguoi dung dang nhap (theo JWT)")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Lay ho so tai khoan hien tai")
    public ApiResponse<ProfileDTO> getProfile(Authentication auth) {
        return ApiResponse.ok("Ho so tai khoan", profileService.getProfile(auth.getName()));
    }

    @PutMapping
    @Operation(summary = "Cap nhat ho so (ho ten, so dien thoai, email)")
    public ApiResponse<ProfileDTO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                 Authentication auth) {
        return ApiResponse.ok("Cap nhat thanh cong", profileService.updateProfile(auth.getName(), request));
    }
}
