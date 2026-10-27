package com.zaalima.ecommerce.payment.messaging;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.events.InventoryReservedEvent;
import com.zaalima.ecommerce.events.OrderCancelledEvent;
import com.zaalima.ecommerce.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {Topics.INVENTORY_EVENTS, Topics.ORDER_EVENTS}, groupId = "payment-service")
public class PaymentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaListener.class);

    private final PaymentService service;

    public PaymentSagaListener(PaymentService service) {
        this.service = service;
    }

    @KafkaHandler
    public void on(InventoryReservedEvent event) {
        log.info("Received InventoryReserved for order {} -> charging", event.getOrderId());
        service.charge(event);
    }

    @KafkaHandler
    public void on(OrderCancelledEvent event) {
        log.info("Received OrderCancelled for order {} -> refunding if paid", event.getOrderId());
        service.onOrderCancelled(event);
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object other) {
        log.debug("Ignoring event of type {}", other.getClass().getSimpleName());
    }
}
