package com.zaalima.ecommerce.order.web.errors;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String id) {
        super("Order " + id + " not found");
    }
}
