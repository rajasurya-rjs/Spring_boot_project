package com.example.in_class_project.controller;

import com.example.in_class_project.dto.AddToCartRequest;
import com.example.in_class_project.model.CartItem;
import com.example.in_class_project.model.Product;
import com.example.in_class_project.service.CartService;
import com.example.in_class_project.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    public CartController(CartService cartService, ProductService productService) {
        this.cartService = cartService;
        this.productService = productService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AddToCartRequest req) {
        try {
            CartItem item = cartService.add(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Map<String,Object>>> get(@PathVariable String userId) {
        List<CartItem> list = cartService.getUserCart(userId);
        List<Map<String,Object>> out = new ArrayList<>();
        for (CartItem c : list) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("productId", c.getProductId());
            m.put("quantity", c.getQuantity());

            Map<String,Object> prodMap = new LinkedHashMap<>();
            Optional<Product> p = productService.get(c.getProductId());
            if (p.isPresent()) {
                prodMap.put("id", p.get().getId());
                prodMap.put("name", p.get().getName());
                prodMap.put("price", p.get().getPrice());
            }
            m.put("product", prodMap);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Map<String,String>> clear(@PathVariable String userId) {
        cartService.clear(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<Map<String,String>> remove(@PathVariable String cartItemId) {
        cartService.removeItem(cartItemId);
        return ResponseEntity.ok(Map.of("message", "Item removed"));
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> updateQty(@PathVariable String cartItemId, @RequestBody Map<String,Integer> body) {
        Integer q = body.get("quantity");
        if (q == null || q <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid quantity"));
        }
        try {
            CartItem updated = cartService.updateQty(cartItemId, q);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<Map<String,Object>> total(@PathVariable String userId) {
        double t = cartService.calcTotal(userId);
        return ResponseEntity.ok(Map.of("total", t));
    }
}
