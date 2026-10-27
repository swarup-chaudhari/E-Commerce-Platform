package com.zaalima.ecommerce.inventory.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CheckRequest(@NotEmpty @Valid List<Item> items) {
    public record Item(@NotBlank String productId, @Min(1) int quantity) {}
}
