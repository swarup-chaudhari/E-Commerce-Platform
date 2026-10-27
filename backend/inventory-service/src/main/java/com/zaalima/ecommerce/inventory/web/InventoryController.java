package com.zaalima.ecommerce.inventory.web;

import com.zaalima.ecommerce.inventory.domain.StockItem;
import com.zaalima.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    /** Called synchronously by order-service (guarded there by Resilience4j). */
    @PostMapping("/check")
    public AvailabilityResponse check(@Valid @RequestBody CheckRequest request) {
        return service.check(request);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> get(@PathVariable String productId) {
        StockItem item = service.getStock(productId);
        return item == null ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(new StockResponse(item.getProductId(), item.getAvailableQuantity()));
    }

    /** ADMIN only (enforced at the gateway). */
    @PutMapping("/{productId}/stock")
    public StockResponse setStock(@PathVariable String productId, @Valid @RequestBody StockUpdateRequest body) {
        StockItem item = service.setStock(productId, body.quantity());
        return new StockResponse(item.getProductId(), item.getAvailableQuantity());
    }
}
