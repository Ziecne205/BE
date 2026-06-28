package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/fee-config")
@RequiredArgsConstructor
@Tag(name = "Manager - Fee Config", description = "Cau hinh tai chinh he thong (A5)")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class FeeConfigController {

    private final FeeConfigService feeConfigService;

    @GetMapping
    @Operation(summary = "Lay cau hinh tai chinh hien tai")
    public ApiResponse<FeeConfigDTO> getFeeConfig() {
        return ApiResponse.ok("Thanh cong", feeConfigService.getFeeConfig());
    }

    @PutMapping
    @Operation(summary = "Cap nhat cau hinh tai chinh")
    public ApiResponse<FeeConfigDTO> updateFeeConfig(@RequestBody FeeConfigDTO request) {
        return ApiResponse.ok("Cap nhat thanh cong", feeConfigService.updateFeeConfig(request));
    }
}
