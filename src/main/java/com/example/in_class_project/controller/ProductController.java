package com.example.in_class_project.controller;

import com.example.in_class_project.model.Product;
import com.example.in_class_project.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService svc;

    public ProductController(ProductService svc) {
        this.svc = svc;
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product p) {
        if (p.getName() == null || p.getName().isBlank() ||
                p.getPrice() == null || p.getStock() == null) {
            return ResponseEntity.badRequest().build();
        }
        Product saved = svc.create(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Product>> all() {
        return ResponseEntity.ok(svc.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> one(@PathVariable String id) {
        Optional<Product> p = svc.get(id);
        return p.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody Product in) {
        Optional<Product> p = svc.update(id, in);
        return p.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean ok = svc.delete(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam("q") String q) {
        return ResponseEntity.ok(svc.search(q));
    }
}
