package com.example.in_class_project.webhook;

import com.example.in_class_project.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public ResponseEntity<?> handle(@RequestBody Map<String,Object> body) {
        try {
            String event = (String) body.get("event");
            Map<String,Object> payload = (Map<String,Object>) body.get("payload");
            Map<String,Object> payment = (Map<String,Object>) payload.get("payment");

            String paymentId = (String) payment.get("id");
            String orderId = (String) payment.get("order_id");
            String status = (String) payment.get("status");

            if ("payment.captured".equals(event) || "payment.authorized".equals(event)) {
                paymentService.handleWebhook(paymentId, orderId, "captured");
            } else if ("payment.failed".equals(event)) {
                paymentService.handleWebhook(paymentId, orderId, "failed");
            }
            return ResponseEntity.ok(Map.of("message", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
