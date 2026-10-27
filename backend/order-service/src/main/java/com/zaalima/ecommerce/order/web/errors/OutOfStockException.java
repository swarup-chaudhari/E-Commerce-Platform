package com.zaalima.ecommerce.order.web.errors;

import java.util.List;

public class OutOfStockException extends RuntimeException {
    private final List<String> productIds;

    public OutOfStockException(List<String> productIds) {
        super("Insufficient stock for: " + String.join(", ", productIds));
        this.productIds = productIds;
    }

    public List<String> getProductIds() {
        return productIds;
    }
}
