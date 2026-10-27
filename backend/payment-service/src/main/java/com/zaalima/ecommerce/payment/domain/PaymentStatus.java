package com.zaalima.ecommerce.payment.domain;

public enum PaymentStatus {
    COMPLETED,
    FAILED,
    REFUNDED,
    /** Tombstone: the order was cancelled before payment ran, so a late InventoryReserved must not charge. */
    CANCELLED
}
