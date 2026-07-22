package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.Payment;
import com.parking.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Xem giao dich thanh toan - hien chi phuc vu hang cho hoan coc thu cong
 * (refundStatus=ManualRequired) khi PayOS khong co Payout duoc cau hinh (Phase 6.1).
 */
@RestController
@RequestMapping("/api/manager/payments")
@RequiredArgsConstructor
@Tag(name = "Manager - Payments", description = "Xem giao dich thanh toan / hang cho hoan coc thu cong (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class PaymentManagerController {

    private final PaymentRepository paymentRepository;

    @GetMapping
    @Operation(summary = "Loc giao dich theo refundStatus (vd ManualRequired = can hoan coc thu cong)")
    public ApiResponse<List<Payment>> findAll(@RequestParam(required = false) String refundStatus) {
        if (refundStatus != null) {
            return ApiResponse.ok("Giao dich theo refundStatus", paymentRepository.findByRefundStatus(refundStatus));
        }
        return ApiResponse.ok("Tat ca giao dich", paymentRepository.findAll());
    }
}
