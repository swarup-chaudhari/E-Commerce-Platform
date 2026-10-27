package com.zaalima.ecommerce.order.web;

import com.zaalima.ecommerce.order.domain.CustomerOrder;
import com.zaalima.ecommerce.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(String id, String customerId, OrderStatus status, long totalAmountCents,
                            String failureReason, Instant createdAt, Instant updatedAt,
                            List<CreateOrderRequest.Item> items) {

    public static OrderResponse from(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus(), o.getTotalAmountCents(),
                o.getFailureReason(), o.getCreatedAt(), o.getUpdatedAt(),
                o.getItems().stream()
                        .map(i -> new CreateOrderRequest.Item(i.getProductId(), i.getQuantity(), i.getUnitPriceCents()))
                        .toList());
    }
}
