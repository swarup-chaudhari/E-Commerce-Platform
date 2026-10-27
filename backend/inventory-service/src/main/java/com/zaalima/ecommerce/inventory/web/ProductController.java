package com.zaalima.ecommerce.inventory.web;

import com.zaalima.ecommerce.inventory.domain.Product;
import com.zaalima.ecommerce.inventory.repository.mongo.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

/** Product catalog (MongoDB). Writes are ADMIN-only at the gateway. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return category == null ? products.findAll() : products.findByCategory(category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable String id) {
        return products.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        if (products.existsById(request.id())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Product saved = products.save(toProduct(request.id(), request));
        return ResponseEntity.created(URI.create("/api/products/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return products.save(toProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        products.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Product toProduct(String id, ProductRequest r) {
        Product p = new Product();
        p.setId(id);
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPriceCents(r.priceCents());
        p.setCategory(r.category());
        p.setAttributes(r.attributes() == null ? new HashMap<>() : new HashMap<>(r.attributes()));
        return p;
    }
}
