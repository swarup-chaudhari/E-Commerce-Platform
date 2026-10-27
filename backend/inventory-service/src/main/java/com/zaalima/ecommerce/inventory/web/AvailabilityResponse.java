package com.zaalima.ecommerce.inventory.web;

import java.util.List;

public record AvailabilityResponse(boolean checked, List<String> unavailableProductIds) {}
