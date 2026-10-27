package com.zaalima.ecommerce.order.messaging;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.events.InventoryReservationFailedEvent;
import com.zaalima.ecommerce.events.PaymentCompletedEvent;
import com.zaalima.ecommerce.events.PaymentFailedEvent;
import com.zaalima.ecommerce.order.service.OrderSagaHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumer group "order-service": records for one orderId always land on one partition, in order. */
@Component
@KafkaListener(topics = {Topics.INVENTORY_EVENTS, Topics.PAYMENT_EVENTS}, groupId = "order-service")
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderSagaHandler handler;

    public OrderSagaListener(OrderSagaHandler handler) {
        this.handler = handler;
    }

    @KafkaHandler
    public void on(InventoryReservationFailedEvent event) {
        log.info("Received InventoryReservationFailed for order {}", event.getOrderId());
        handler.onInventoryReservationFailed(event);
    }

    @KafkaHandler
    public void on(PaymentCompletedEvent event) {
        log.info("Received PaymentCompleted for order {}", event.getOrderId());
        handler.onPaymentCompleted(event);
    }

    @KafkaHandler
    public void on(PaymentFailedEvent event) {
        log.info("Received PaymentFailed for order {}", event.getOrderId());
        handler.onPaymentFailed(event);
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object other) {
        log.debug("Ignoring event of type {}", other.getClass().getSimpleName());
    }
}
