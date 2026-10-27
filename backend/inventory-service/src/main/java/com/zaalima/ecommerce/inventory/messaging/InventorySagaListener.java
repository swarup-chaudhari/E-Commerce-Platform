package com.zaalima.ecommerce.inventory.messaging;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.events.OrderCancelledEvent;
import com.zaalima.ecommerce.events.OrderCreatedEvent;
import com.zaalima.ecommerce.events.PaymentFailedEvent;
import com.zaalima.ecommerce.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {Topics.ORDER_EVENTS, Topics.PAYMENT_EVENTS}, groupId = "inventory-service")
public class InventorySagaListener {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaListener.class);

    private final InventoryService service;

    public InventorySagaListener(InventoryService service) {
        this.service = service;
    }

    @KafkaHandler
    public void on(OrderCreatedEvent event) {
        log.info("Received OrderCreated for order {}", event.getOrderId());
        service.reserve(event);
    }

    /** Compensation: payment failed after stock was deducted. */
    @KafkaHandler
    public void on(PaymentFailedEvent event) {
        log.info("Received PaymentFailed for order {} -> releasing stock", event.getOrderId());
        service.release(event.getEventId(), event.getOrderId(), "payment failed");
    }

    /** Compensation: customer cancelled after stock was deducted. */
    @KafkaHandler
    public void on(OrderCancelledEvent event) {
        log.info("Received OrderCancelled for order {} -> releasing stock", event.getOrderId());
        service.release(event.getEventId(), event.getOrderId(), "order cancelled");
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object other) {
        log.debug("Ignoring event of type {}", other.getClass().getSimpleName());
    }
}
