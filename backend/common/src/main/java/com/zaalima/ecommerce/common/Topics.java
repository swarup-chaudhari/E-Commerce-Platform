package com.zaalima.ecommerce.common;

/** Kafka topic names. Partition key is always the orderId, which guarantees per-order ordering. */
public final class Topics {
    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String PAYMENT_EVENTS = "payment-events";

    private Topics() {}
}
