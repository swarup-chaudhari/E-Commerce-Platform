package com.zaalima.ecommerce.inventory.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ProductRequest(@NotBlank String id, @NotBlank String name, String description,
                             @Min(0) long priceCents, String category, Map<String, String> attributes) {}
