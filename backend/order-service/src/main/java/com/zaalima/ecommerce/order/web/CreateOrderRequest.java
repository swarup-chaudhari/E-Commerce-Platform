package com.zaalima.ecommerce.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(@NotEmpty @Valid List<Item> items) {

    public record Item(@NotBlank String productId,
                       @Min(1) int quantity,
                       @Min(0) long unitPriceCents) {}
}
