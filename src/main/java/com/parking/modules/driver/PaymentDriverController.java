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
    private final PayosService payosService;

    @PostMapping("/payos/create-link")
    public ApiResponse<PayosLinkResponse> createPayosLink(@RequestBody PayosLinkRequest request, Authentication auth) {
        if (!"DEPOSIT".equalsIgnoreCase(request.getType())) {
            return ApiResponse.fail("Hien chi ho tro thanh toan coc (DEPOSIT)");
        }
        return ApiResponse.ok("Tao link PayOS thanh cong",
                payosService.createDepositLink(request.getId(), auth.getName()));
    }

    @PostMapping("/checkout")
    public ApiResponse<String> checkout(@RequestBody PaymentRequest request, Authentication auth) {
        return ApiResponse.ok("URL thanh toan duoc tao thanh cong", paymentDriverService.createMockPaymentUrl(request, auth.getName()));
    }

    @PostMapping("/mock-callback")
    public ApiResponse<Payment> mockCallback(@RequestParam String txnRef, @RequestParam Long sessionId, @RequestParam String status) {
        return ApiResponse.ok("Cap nhat thanh toan thanh cong", paymentDriverService.processMockCallback(txnRef, sessionId, status));
    }
}
