package com.parking.modules.manualrefund;

import com.parking.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manager/manual-refunds")
@RequiredArgsConstructor
public class ManualRefundAdminController {

    private final ManualRefundService manualRefundService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<List<ManualRefundResponse>> getAllRefundRequests() {
        List<ManualRefundResponse> list = manualRefundService.getAllRefundRequests();
        return ApiResponse.ok("Lấy danh sách yêu cầu hoàn cọc thành công", list);
    }

    @PatchMapping("/{id}/mark-processed")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<ManualRefundResponse> markAsProcessed(@PathVariable UUID id) {
        ManualRefundResponse response = manualRefundService.markAsProcessed(id);
        return ApiResponse.ok("Đánh dấu đã xử lý thành công", response);
    }
}
