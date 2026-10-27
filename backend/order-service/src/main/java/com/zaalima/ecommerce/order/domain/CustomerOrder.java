package com.zaalima.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Order aggregate: current-state projection of the order's event stream (see OrderEventRecord). */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class CustomerOrder {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private long totalAmountCents;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    public static CustomerOrder create(String customerId, List<OrderItem> items) {
        CustomerOrder o = new CustomerOrder();
        o.id = UUID.randomUUID().toString();
        o.customerId = customerId;
        o.items = new ArrayList<>(items);
        o.totalAmountCents = items.stream().mapToLong(i -> i.getUnitPriceCents() * i.getQuantity()).sum();
        o.status = OrderStatus.PENDING;
        o.createdAt = Instant.now();
        o.updatedAt = o.createdAt;
        return o;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public void confirm() {
        requirePending("confirm");
        status = OrderStatus.CONFIRMED;
        touch();
    }

    public void reject(String reason) {
        requirePending("reject");
        status = OrderStatus.REJECTED;
        failureReason = reason;
        touch();
    }

    /** Customers may cancel while the order is pending or already confirmed. */
    public void cancel(String reason) {
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order " + id + " is " + status + " and cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
        failureReason = reason;
        touch();
    }

    private void requirePending(String action) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot " + action + " order " + id + " in status " + status);
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
