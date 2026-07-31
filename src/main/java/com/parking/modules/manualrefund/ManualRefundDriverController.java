package com.parking.modules.manualrefund;

import com.parking.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/manual-refunds")
@RequiredArgsConstructor
public class ManualRefundDriverController {

    private final ManualRefundService manualRefundService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<ManualRefundResponse> submitRefundRequest(
            @Valid @RequestBody ManualRefundSubmitRequest request,
            Authentication authentication) {
        
        ManualRefundResponse response = manualRefundService.createManualRefundRequest(request, authentication.getName());
        return ApiResponse.ok("Gửi yêu cầu hoàn cọc thành công", response);
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<ManualRefundResponse>> getMyRefundRequests(Authentication authentication) {
        List<ManualRefundResponse> list = manualRefundService.getMyRefundRequests(authentication.getName());
        return ApiResponse.ok("Lấy danh sách yêu cầu hoàn cọc thành công", list);
    }
}
