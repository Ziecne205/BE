package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/fee-config")
@RequiredArgsConstructor
public class FeeConfigController {

    private final FeeConfigService feeConfigService;

    @GetMapping
    public ApiResponse<FeeConfigResponse> getFeeConfig() {
        return ApiResponse.ok(feeConfigService.getFeeConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<FeeConfigResponse> updateFeeConfig(@Valid @RequestBody FeeConfigRequest request) {
        return ApiResponse.ok("Cap nhat thanh cong", feeConfigService.updateFeeConfig(request));
    }
}
