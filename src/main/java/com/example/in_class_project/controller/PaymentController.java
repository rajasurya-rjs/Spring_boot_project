package com.example.in_class_project.controller;

import com.example.in_class_project.dto.PaymentRequest;
import com.example.in_class_project.model.Payment;
import com.example.in_class_project.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody PaymentRequest req) {
        try {
            Payment p = paymentService.createPayment(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "paymentId", p.getId(),
                    "orderId", p.getOrderId(),
                    "amount", p.getAmount(),
                    "status", p.getStatus(),
                    "razorpayOrderId", p.getRazorpayOrderId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<?> get(@PathVariable String paymentId) {
        try {
            Payment p = paymentService.getById(paymentId);
            return ResponseEntity.ok(Map.of(
                    "id", p.getId(),
                    "orderId", p.getOrderId(),
                    "amount", p.getAmount(),
                    "status", p.getStatus(),
                    "paymentId", p.getPaymentId(),
                    "razorpayOrderId", p.getRazorpayOrderId(),
                    "createdAt", p.getCreatedAt()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
