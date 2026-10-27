package com.zaalima.ecommerce.order.web;

import com.zaalima.ecommerce.order.domain.OrderEventRecord;

import java.time.Instant;

public record OrderEventResponse(Long sequence, String eventType, String payload, Instant occurredAt) {
    public static OrderEventResponse from(OrderEventRecord r) {
        return new OrderEventResponse(r.getId(), r.getEventType(), r.getPayload(), r.getOccurredAt());
    }
}
