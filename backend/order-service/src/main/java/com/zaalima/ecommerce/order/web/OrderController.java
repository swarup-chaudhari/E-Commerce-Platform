package com.zaalima.ecommerce.order.web;

import com.zaalima.ecommerce.order.domain.CustomerOrder;
import com.zaalima.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** X-User-Id / X-User-Roles are injected by the API gateway after JWT validation. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestHeader("X-User-Id") String userId,
                                                @Valid @RequestBody CreateOrderRequest request) {
        CustomerOrder order = service.createOrder(userId, request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping
    public List<OrderResponse> listMine(@RequestHeader("X-User-Id") String userId) {
        return service.listMyOrders(userId).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id,
                             @RequestHeader("X-User-Id") String userId,
                             @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return OrderResponse.from(service.getOrder(id, userId, roles));
    }

    @GetMapping("/{id}/events")
    public List<OrderEventResponse> events(@PathVariable String id,
                                           @RequestHeader("X-User-Id") String userId,
                                           @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return service.history(id, userId, roles).stream().map(OrderEventResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable String id,
                                @RequestHeader("X-User-Id") String userId,
                                @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return OrderResponse.from(service.cancel(id, userId, roles));
    }
}
