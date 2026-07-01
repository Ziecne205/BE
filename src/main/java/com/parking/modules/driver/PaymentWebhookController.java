package com.parking.modules.driver;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint cong khai cho PayOS goi ve (webhook) khi thanh toan hoan tat.
 * Luu y: PayOS khong goi duoc vao localhost — can URL public (ngrok...) khi test that.
 */
@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PayosService payosService;

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody JsonNode body) {
        try {
            payosService.handleWebhook(body);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            // Khong nem 500 de PayOS khong retry vo han khi la ping/test.
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
