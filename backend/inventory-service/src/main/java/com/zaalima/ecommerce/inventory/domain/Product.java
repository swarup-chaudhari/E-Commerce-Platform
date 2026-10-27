package com.zaalima.ecommerce.inventory.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/** Product catalog entry (flexible, schema-less attributes => MongoDB). */
@Document("products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private long priceCents;
    private String category;
    private Map<String, String> attributes = new HashMap<>();
}
