package com.zaalima.ecommerce.inventory.web;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(0) int quantity) {}
