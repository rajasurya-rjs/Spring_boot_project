package com.example.in_class_project.repository;

import com.example.in_class_project.model.CartItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<CartItem, String> {

    List<CartItem> findByUserId(String userId);

    void deleteByUserId(String userId);

    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);
}
