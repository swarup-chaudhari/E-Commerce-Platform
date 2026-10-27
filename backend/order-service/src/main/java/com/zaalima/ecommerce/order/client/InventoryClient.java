package com.zaalima.ecommerce.order.client;

import com.zaalima.ecommerce.order.domain.OrderItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Synchronous fast-fail stock pre-check. Wrapped (outermost to innermost) in
 * Retry -> CircuitBreaker -> RateLimiter. If inventory is down the fallback lets the order
 * through as PENDING - the Kafka saga then reserves stock when inventory recovers, so no
 * revenue is lost.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    record CheckItem(String productId, int quantity) {}

    record CheckRequest(List<CheckItem> items) {}

    private final RestClient restClient;

    public InventoryClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://inventory-service").build();
    }

    @Retry(name = "inventory", fallbackMethod = "checkFallback")
    @CircuitBreaker(name = "inventory")
    @RateLimiter(name = "inventory")
    public AvailabilityResult checkAvailability(List<OrderItem> items) {
        CheckRequest body = new CheckRequest(
                items.stream().map(i -> new CheckItem(i.getProductId(), i.getQuantity())).toList());
        return restClient.post()
                .uri("/api/inventory/check")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AvailabilityResult.class);
    }

    @SuppressWarnings("unused")
    public AvailabilityResult checkFallback(List<OrderItem> items, Throwable t) {
        log.warn("Inventory pre-check unavailable ({}); accepting order as PENDING for async saga", t.toString());
        return AvailabilityResult.unchecked();
    }
}
