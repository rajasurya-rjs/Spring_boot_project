package com.example.in_class_project.service;

import com.example.in_class_project.dto.PaymentRequest;
import com.example.in_class_project.model.Order;
import com.example.in_class_project.model.Payment;
import com.example.in_class_project.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderService orderService;
    private final RazorpayClient razorpayClient;

    public PaymentService(PaymentRepository paymentRepo,
                          OrderService orderService,
                          RazorpayClient razorpayClient) {
        this.paymentRepo = paymentRepo;
        this.orderService = orderService;
        this.razorpayClient = razorpayClient;
    }

    public Payment createPayment(PaymentRequest req) {
        Order order = orderService.getOrder(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("order not found"));
        if (!"CREATED".equals(order.getStatus()))
            throw new RuntimeException("order not payable in status " + order.getStatus());

        double amount = order.getTotalAmount();
        try {
            JSONObject body = new JSONObject();
            body.put("amount", (long) (amount * 100));
            body.put("currency", "INR");
            body.put("receipt", order.getId());
            JSONObject notes = new JSONObject();
            notes.put("orderId", order.getId());
            notes.put("userId", order.getUserId());
            body.put("notes", notes);

            com.razorpay.Order rpOrder = razorpayClient.orders.create(body);
            String rpOrderId = rpOrder.get("id");

            Payment p = new Payment();
            p.setId(UUID.randomUUID().toString());
            p.setOrderId(order.getId());
            p.setAmount(amount);
            p.setStatus("PENDING");
            p.setRazorpayOrderId(rpOrderId);
            p.setCreatedAt(Instant.now());

            return paymentRepo.save(p);
        } catch (Exception e) {
            throw new RuntimeException("razorpay error: " + e.getMessage());
        }
    }

    public void handleWebhook(String paymentId, String razorpayOrderId, String status) {
        Optional<Payment> pOpt = paymentRepo.findByRazorpayOrderId(razorpayOrderId);
        if (pOpt.isEmpty()) throw new RuntimeException("payment not found for order " + razorpayOrderId);
        Payment p = pOpt.get();

        if ("captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status)) {
            p.setStatus("SUCCESS");
            p.setPaymentId(paymentId);
            orderService.updateStatus(p.getOrderId(), "PAID");
        } else if ("failed".equalsIgnoreCase(status)) {
            p.setStatus("FAILED");
            orderService.updateStatus(p.getOrderId(), "FAILED");
        }
        paymentRepo.save(p);
    }

    public Optional<Payment> getByOrderId(String orderId) {
        return paymentRepo.findByOrderId(orderId);
    }

    public Payment getById(String id) {
        return paymentRepo.findById(id).orElseThrow(() -> new RuntimeException("payment not found"));
    }
}
