package com.zaalima.ecommerce.inventory;

import com.zaalima.ecommerce.inventory.domain.Product;
import com.zaalima.ecommerce.inventory.repository.mongo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Seeds a demo catalog on first start (stock rows are seeded by Flyway V2). */
@Component
public class CatalogSeeder implements CommandLineRunner {

    private final ProductRepository products;

    public CatalogSeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;
        products.save(product("P-1001", "Wireless Headphones", 4999, "audio", Map.of("color", "black", "bluetooth", "5.3")));
        products.save(product("P-1002", "Mechanical Keyboard", 8999, "peripherals", Map.of("switch", "brown", "layout", "ANSI")));
        products.save(product("P-1003", "USB-C Hub", 2999, "accessories", Map.of("ports", "7")));
    }

    private Product product(String id, String name, long priceCents, String category, Map<String, String> attrs) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPriceCents(priceCents);
        p.setCategory(category);
        p.setAttributes(new java.util.HashMap<>(attrs));
        return p;
    }
}
