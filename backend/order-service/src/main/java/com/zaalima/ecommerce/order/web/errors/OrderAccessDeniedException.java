package com.zaalima.ecommerce.order.web.errors;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(String id) {
        super("You do not have access to order " + id);
    }
}
