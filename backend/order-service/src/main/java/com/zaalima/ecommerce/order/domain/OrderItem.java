package com.zaalima.ecommerce.order.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private int quantity;
    private long unitPriceCents;
}
