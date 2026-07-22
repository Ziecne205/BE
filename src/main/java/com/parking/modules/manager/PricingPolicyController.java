package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.PricingPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/pricing-policies")
@RequiredArgsConstructor
@Tag(name = "Manager - Pricing Policies", description = "Cau hinh chinh sach gia gui xe (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class PricingPolicyController {

    private final PricingPolicyService pricingPolicyService;

    @GetMapping
    @Operation(summary = "Lay danh sach tat ca chinh sach gia")
    public ApiResponse<List<PricingPolicy>> findAll() {
        return ApiResponse.ok("Danh sach chinh sach gia", pricingPolicyService.findAll());
    }

    @GetMapping("/vehicle-type/{vehicleTypeId}")
    @Operation(summary = "Lay lich su bang gia theo loai xe (moi nhat o tren)")
    public ApiResponse<List<PricingPolicy>> findByVehicleType(@PathVariable Integer vehicleTypeId) {
        return ApiResponse.ok("Bang gia theo loai xe", pricingPolicyService.findByVehicleType(vehicleTypeId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet mot chinh sach gia")
    public ApiResponse<PricingPolicy> findById(@PathVariable Integer id) {
        return ApiResponse.ok("Chi tiet chinh sach gia", pricingPolicyService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tao chinh sach gia moi (tu dong Expired chinh sach cu cung loai xe)")
    public ApiResponse<PricingPolicy> create(@Valid @RequestBody PricingPolicyRequest request, Authentication auth) {
        return ApiResponse.ok("Tao chinh sach gia thanh cong", pricingPolicyService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cap nhat chinh sach gia")
    public ApiResponse<PricingPolicy> update(@PathVariable Integer id,
                                             @Valid @RequestBody PricingPolicyRequest request,
                                             Authentication auth) {
        return ApiResponse.ok("Cap nhat chinh sach gia thanh cong",
                pricingPolicyService.update(id, request, auth.getName()));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Huy kich hoat (Expired) mot chinh sach gia")
    public ApiResponse<PricingPolicy> deactivate(@PathVariable Integer id, Authentication auth) {
        return ApiResponse.ok("Da huy kich hoat chinh sach gia", pricingPolicyService.deactivate(id, auth.getName()));
    }
}
