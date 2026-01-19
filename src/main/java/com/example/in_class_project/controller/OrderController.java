package com.example.in_class_project.controller;

import com.example.in_class_project.dto.CreateOrderRequest;
import com.example.in_class_project.model.Order;
import com.example.in_class_project.model.OrderItem;
import com.example.in_class_project.model.Payment;
import com.example.in_class_project.service.OrderService;
import com.example.in_class_project.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateOrderRequest req) {
        try {
            Order o = orderService.createOrder(req);
            List<OrderItem> items = orderService.getOrderItems(o.getId());
            Map<String,Object> res = new LinkedHashMap<>();
            res.put("id", o.getId());
            res.put("userId", o.getUserId());
            res.put("totalAmount", o.getTotalAmount());
            res.put("status", o.getStatus());
            List<Map<String,Object>> it = new ArrayList<>();
            for (OrderItem oi : items) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("productId", oi.getProductId());
                m.put("quantity", oi.getQuantity());
                m.put("price", oi.getPrice());
                it.add(m);
            }
            res.put("items", it);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> get(@PathVariable String orderId) {
        try {
            Optional<Order> o = orderService.getOrder(orderId);
            if (o.isEmpty()) return ResponseEntity.notFound().build();
            Order ord = o.get();
            List<OrderItem> items = orderService.getOrderItems(orderId);
            Optional<Payment> payOpt = paymentService.getByOrderId(orderId);

            Map<String,Object> res = new LinkedHashMap<>();
            res.put("id", ord.getId());
            res.put("userId", ord.getUserId());
            res.put("totalAmount", ord.getTotalAmount());
            res.put("status", ord.getStatus());
            res.put("createdAt", ord.getCreatedAt());

            List<Map<String,Object>> it = new ArrayList<>();
            for (OrderItem oi : items) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("productId", oi.getProductId());
                m.put("quantity", oi.getQuantity());
                m.put("price", oi.getPrice());
                it.add(m);
            }
            res.put("items", it);

            if (payOpt.isPresent()) {
                Payment p = payOpt.get();
                Map<String,Object> pm = new LinkedHashMap<>();
                pm.put("id", p.getId());
                pm.put("status", p.getStatus());
                pm.put("amount", p.getAmount());
                pm.put("paymentId", p.getPaymentId());
                res.put("payment", pm);
            }

            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> userOrders(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String orderId) {
        try {
            Order o = orderService.cancel(orderId);
            return ResponseEntity.ok(Map.of(
                    "message", "Order cancelled successfully",
                    "orderId", o.getId(),
                    "status", o.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
