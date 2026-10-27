package com.zaalima.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Append-only event store row. Never updated or deleted. */
@Entity
@Table(name = "order_events")
@Getter
@NoArgsConstructor
public class OrderEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant occurredAt;

    public OrderEventRecord(String orderId, String eventType, String payload, Instant occurredAt) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }
}
