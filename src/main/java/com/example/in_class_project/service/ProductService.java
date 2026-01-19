package com.example.in_class_project.service;

import com.example.in_class_project.model.Product;
import com.example.in_class_project.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public Product create(Product p) {
        return repo.save(p);
    }

    public List<Product> getAll() {
        return repo.findAll();
    }

    public Optional<Product> get(String id) {
        return repo.findById(id);
    }

    public Optional<Product> update(String id, Product input) {
        return repo.findById(id).map(p -> {
            if (input.getName() != null) p.setName(input.getName());
            if (input.getDescription() != null) p.setDescription(input.getDescription());
            if (input.getPrice() != null) p.setPrice(input.getPrice());
            if (input.getStock() != null) p.setStock(input.getStock());
            return repo.save(p);
        });
    }

    public boolean delete(String id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    public List<Product> search(String q) {
        return repo.findByNameContainingIgnoreCase(q);
    }
}
