package com.zaalima.ecommerce.order.client;

import java.util.List;

/** checked=false means the inventory service could not be reached (fail-open: the saga re-validates). */
public record AvailabilityResult(boolean checked, List<String> unavailableProductIds) {
    public static AvailabilityResult unchecked() {
        return new AvailabilityResult(false, List.of());
    }
}
