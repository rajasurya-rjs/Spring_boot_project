package com.example.in_class_project.service;

import com.example.in_class_project.dto.AddToCartRequest;
import com.example.in_class_project.model.CartItem;
import com.example.in_class_project.model.Product;
import com.example.in_class_project.repository.CartRepository;
import com.example.in_class_project.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;

    public CartService(CartRepository cartRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    public CartItem add(AddToCartRequest req) {
        Optional<Product> p = productRepo.findById(req.getProductId());
        if (p.isEmpty()) throw new RuntimeException("product not found");
        Product prod = p.get();
        if (prod.getStock() == null || prod.getStock() < req.getQuantity())
            throw new RuntimeException("not enough stock");

        Optional<CartItem> existing = cartRepo.findByUserIdAndProductId(req.getUserId(), req.getProductId());
        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            item = new CartItem();
            item.setUserId(req.getUserId());
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity());
        }
        return cartRepo.save(item);
    }

    public List<CartItem> getUserCart(String userId) {
        return cartRepo.findByUserId(userId);
    }

    public void clear(String userId) {
        cartRepo.deleteByUserId(userId);
    }

    public void removeItem(String cartItemId) {
        cartRepo.deleteById(cartItemId);
    }

    public CartItem updateQty(String cartItemId, int qty) {
        Optional<CartItem> c = cartRepo.findById(cartItemId);
        if (c.isEmpty()) throw new RuntimeException("cart item not found");
        CartItem item = c.get();
        item.setQuantity(qty);
        return cartRepo.save(item);
    }

    public double calcTotal(String userId) {
        List<CartItem> items = cartRepo.findByUserId(userId);
        double total = 0;
        for (CartItem ci : items) {
            Optional<Product> p = productRepo.findById(ci.getProductId());
            if (p.isPresent() && p.get().getPrice() != null) {
                total += p.get().getPrice() * ci.getQuantity();
            }
        }
        return total;
    }
}
