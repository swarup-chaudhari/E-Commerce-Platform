package com.zaalima.ecommerce.order.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerOrderTest {

    private CustomerOrder newOrder() {
        return CustomerOrder.create("alice", List.of(
                new OrderItem("P-1", 2, 1500),
                new OrderItem("P-2", 1, 500)));
    }

    @Test
    void newOrderIsPendingWithComputedTotal() {
        CustomerOrder order = newOrder();
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(3500, order.getTotalAmountCents());
    }

    @Test
    void pendingOrderCanBeConfirmedOnce() {
        CustomerOrder order = newOrder();
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertThrows(IllegalStateException.class, order::confirm);
    }

    @Test
    void rejectedOrderKeepsFailureReasonAndCannotBeCancelled() {
        CustomerOrder order = newOrder();
        order.reject("Payment declined");
        assertEquals("Payment declined", order.getFailureReason());
        assertThrows(IllegalStateException.class, () -> order.cancel("changed my mind"));
    }

    @Test
    void confirmedOrderCanBeCancelled() {
        CustomerOrder order = newOrder();
        order.confirm();
        order.cancel("changed my mind");
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
