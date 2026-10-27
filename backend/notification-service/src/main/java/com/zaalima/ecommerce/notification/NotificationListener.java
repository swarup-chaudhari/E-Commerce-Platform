package com.zaalima.ecommerce.notification;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.events.OrderConfirmedEvent;
import com.zaalima.ecommerce.events.OrderCreatedEvent;
import com.zaalima.ecommerce.events.OrderRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Pure consumer: reacts to order lifecycle events; never part of the saga's critical path. */
@Component
@KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "notification-service")
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationSender sender;

    public NotificationListener(NotificationSender sender) {
        this.sender = sender;
    }

    @KafkaHandler
    public void on(OrderCreatedEvent e) {
        sender.send(e.getCustomerId(), "We received your order",
                "Order " + e.getOrderId() + " is being processed.");
    }

    @KafkaHandler
    public void on(OrderConfirmedEvent e) {
        sender.send(e.getCustomerId(), "Order confirmed",
                "Order " + e.getOrderId() + " is confirmed. Thank you!");
    }

    @KafkaHandler
    public void on(OrderRejectedEvent e) {
        sender.send(e.getCustomerId(), "Order could not be completed",
                "Order " + e.getOrderId() + " was rejected: " + e.getReason());
    }

    @KafkaHandler(isDefault = true)
    public void ignore(Object other) {
        log.debug("Ignoring event of type {}", other.getClass().getSimpleName());
    }
}
