package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.CheckoutApprovalRequest;
import com.parking.modules.staff.CheckOutResponse;
import com.parking.modules.staff.SessionService;
import com.parking.repository.CheckoutApprovalRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manager duyet/tu choi cac yeu cau checkout bi tam giu do so tien mat Staff thu tai cong
 * lech qua muc cho phep (CASH_TOLERANCE_VND) so voi so he thong tinh - Phase 6, item 2.
 */
@RestController
@RequestMapping("/api/manager/checkout-approvals")
@RequiredArgsConstructor
@Tag(name = "Manager - Checkout Approvals", description = "Duyet checkout lech tien mat (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class CheckoutApprovalController {

    private final SessionService sessionService;
    private final CheckoutApprovalRequestRepository checkoutApprovalRequestRepository;

    @GetMapping
    @Operation(summary = "Lay danh sach yeu cau duyet checkout (loc theo status, mac dinh Open)")
    public ApiResponse<List<CheckoutApprovalRequest>> findAll(
            @RequestParam(required = false, defaultValue = "Open") String status) {
        return ApiResponse.ok("Danh sach yeu cau duyet checkout",
                checkoutApprovalRequestRepository.findByStatus(status));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Duyet - hoan tat checkout dung nhu Staff da yeu cau ban dau")
    public ApiResponse<CheckOutResponse> approve(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Da duyet, checkout hoan tat", sessionService.approveCheckoutRequest(id, auth.getName()));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Tu choi - phien gui xe van con mo de Staff xu ly lai")
    public ApiResponse<CheckoutApprovalRequest> reject(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Da tu choi yeu cau checkout", sessionService.rejectCheckoutRequest(id, auth.getName()));
    }
}
