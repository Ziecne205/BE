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

    @PostMapping("/payos/create-link")
    public ApiResponse<PayosLinkResponse> createPayosLink(@RequestBody PayosLinkRequest request, Authentication auth) {
        return ApiResponse.ok("URL thanh toan duoc tao thanh cong", paymentDriverService.createPayosLink(request, auth.getName()));
    }

    @PostMapping("/checkout")

    @PostMapping("/mock-callback")
    public ApiResponse<Payment> mockCallback(@RequestParam String txnRef, @RequestParam Long sessionId, @RequestParam String status) {
        return ApiResponse.ok("Cap nhat thanh toan thanh cong", paymentDriverService.processMockCallback(txnRef, sessionId, status));
    }
}
