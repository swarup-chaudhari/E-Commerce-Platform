package com.zaalima.ecommerce.inventory.repository.mongo;

import com.zaalima.ecommerce.inventory.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);
}
