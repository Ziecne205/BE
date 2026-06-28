package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import com.parking.entity.Payment;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/payments")
@RequiredArgsConstructor
@Tag(name = "Driver - Payments", description = "Thanh toan truc tuyen - phan he Khach hang")
@PreAuthorize("hasRole('DRIVER')")
public class PaymentDriverController {

    private final PaymentDriverService paymentDriverService;
    private final ReservationService reservationService;

    @PostMapping("/checkout")
    public ApiResponse<String> checkout(@RequestBody PaymentRequest request, Authentication auth) {
        return ApiResponse.ok("URL thanh toan duoc tao thanh cong", paymentDriverService.createMockPaymentUrl(request, auth.getName()));
    }

    @PostMapping("/mock-callback")
    public ApiResponse<Payment> mockCallback(
            @RequestParam String txnRef, 
            @RequestParam(required = false) Long sessionId, 
            @RequestParam(required = false) Long reservationId,
            @RequestParam String status) {
        
        if (reservationId != null && "Success".equalsIgnoreCase(status)) {
            reservationService.confirmDeposit(reservationId);
            return ApiResponse.ok("Cap nhat thanh toan coc thanh cong", null);
        }
        
        if (sessionId != null) {
            return ApiResponse.ok("Cap nhat thanh toan session thanh cong", paymentDriverService.processMockCallback(txnRef, sessionId, status));
        }
        
        return ApiResponse.fail("Thieu sessionId hoac reservationId");
    }
}
