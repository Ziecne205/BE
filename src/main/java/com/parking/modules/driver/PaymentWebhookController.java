package com.parking.modules.driver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PayOS payOS;
    private final PaymentDriverService paymentDriverService;

    @PostMapping("/webhook")
    public ResponseEntity<?> payosWebhook(@RequestBody ObjectNode body) {
        try {
            WebhookData webhookData = payOS.webhooks().verify(body);
            paymentDriverService.handlePayosWebhook(webhookData);
            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook verified and processed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
