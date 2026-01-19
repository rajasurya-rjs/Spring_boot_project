package com.example.in_class_project.service;

import com.example.in_class_project.dto.CreateOrderRequest;
import com.example.in_class_project.model.CartItem;
import com.example.in_class_project.model.Order;
import com.example.in_class_project.model.OrderItem;
import com.example.in_class_project.model.Product;
import com.example.in_class_project.repository.CartRepository;
import com.example.in_class_project.repository.OrderItemRepository;
import com.example.in_class_project.repository.OrderRepository;
import com.example.in_class_project.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepo,
                        OrderItemRepository orderItemRepo,
                        CartRepository cartRepo,
                        ProductRepository productRepo,
                        CartService cartService) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.cartService = cartService;
    }

    public Order createOrder(CreateOrderRequest req) {
        String userId = req.getUserId();
        List<CartItem> cart = cartRepo.findByUserId(userId);
        if (cart.isEmpty()) throw new RuntimeException("cart empty");

        for (CartItem c : cart) {
            Optional<Product> p = productRepo.findById(c.getProductId());
            if (p.isEmpty()) throw new RuntimeException("product not found: " + c.getProductId());
            if (p.get().getStock() == null || p.get().getStock() < c.getQuantity())
                throw new RuntimeException("stock low for: " + p.get().getName());
        }

        double total = cartService.calcTotal(userId);

        Order o = new Order();
        o.setId(UUID.randomUUID().toString());
        o.setUserId(userId);
        o.setTotalAmount(total);
        o.setStatus("CREATED");
        o.setCreatedAt(Instant.now());
        Order savedOrder = orderRepo.save(o);

        for (CartItem c : cart) {
            Product p = productRepo.findById(c.getProductId()).get();
            OrderItem oi = new OrderItem();
            oi.setId(UUID.randomUUID().toString());
            oi.setOrderId(savedOrder.getId());
            oi.setProductId(c.getProductId());
            oi.setQuantity(c.getQuantity());
            oi.setPrice(p.getPrice());
            orderItemRepo.save(oi);

            p.setStock(p.getStock() - c.getQuantity());
            productRepo.save(p);
        }

        cartService.clear(userId);
        return savedOrder;
    }

    public Optional<Order> getOrder(String id) {
        return orderRepo.findById(id);
    }

    public List<OrderItem> getOrderItems(String orderId) {
        return orderItemRepo.findByOrderId(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepo.findByUserId(userId);
    }

    public Order updateStatus(String orderId, String status) {
        Optional<Order> o = orderRepo.findById(orderId);
        if (o.isEmpty()) throw new RuntimeException("order not found");
        Order ord = o.get();
        ord.setStatus(status);
        return orderRepo.save(ord);
    }

    public Order cancel(String orderId) {
        Optional<Order> o = orderRepo.findById(orderId);
        if (o.isEmpty()) throw new RuntimeException("order not found");
        Order ord = o.get();
        if (!ord.getStatus().equals("CREATED") && !ord.getStatus().equals("FAILED"))
            throw new RuntimeException("cannot cancel in status " + ord.getStatus());

        List<OrderItem> items = orderItemRepo.findByOrderId(orderId);
        for (OrderItem it : items) {
            Optional<Product> p = productRepo.findById(it.getProductId());
            if (p.isPresent()) {
                Product prod = p.get();
                prod.setStock(prod.getStock() + it.getQuantity());
                productRepo.save(prod);
            }
        }
        ord.setStatus("CANCELLED");
        return orderRepo.save(ord);
    }
}
