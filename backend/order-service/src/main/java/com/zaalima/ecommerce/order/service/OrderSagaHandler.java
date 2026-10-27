package com.zaalima.ecommerce.order.service;

import com.zaalima.ecommerce.events.InventoryReservationFailedEvent;
import com.zaalima.ecommerce.events.OrderConfirmedEvent;
import com.zaalima.ecommerce.events.OrderRejectedEvent;
import com.zaalima.ecommerce.events.PaymentCompletedEvent;
import com.zaalima.ecommerce.events.PaymentFailedEvent;
import com.zaalima.ecommerce.order.domain.CustomerOrder;
import com.zaalima.ecommerce.order.domain.ProcessedEvent;
import com.zaalima.ecommerce.order.repository.OrderRepository;
import com.zaalima.ecommerce.order.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Order's reactions in the choreography saga. Terminal states:
 * PaymentCompleted -> CONFIRMED; InventoryReservationFailed / PaymentFailed -> REJECTED.
 * (Stock release on PaymentFailed is the inventory service's own compensating action.)
 */
@Service
public class OrderSagaHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaHandler.class);

    private final OrderRepository orders;
    private final ProcessedEventRepository processed;
    private final OrderEventLog eventLog;

    public OrderSagaHandler(OrderRepository orders, ProcessedEventRepository processed, OrderEventLog eventLog) {
        this.orders = orders;
        this.processed = processed;
        this.eventLog = eventLog;
    }

    @Transactional
    public void onInventoryReservationFailed(InventoryReservationFailedEvent e) {
        if (!firstTime(e.getEventId())) return;
        eventLog.record(e.getOrderId(), e);
        orders.findById(e.getOrderId()).ifPresent(o -> reject(o, e.getReason()));
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent e) {
        if (!firstTime(e.getEventId())) return;
        eventLog.record(e.getOrderId(), e);
        orders.findById(e.getOrderId()).ifPresent(o -> {
            if (!o.isPending()) {
                log.info("Ignoring PaymentCompleted for order {} in status {}", o.getId(), o.getStatus());
                return;
            }
            o.confirm();
            eventLog.recordAndPublish(o.getId(), OrderConfirmedEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setOrderId(o.getId())
                    .setCustomerId(o.getCustomerId())
                    .setOccurredAt(System.currentTimeMillis())
                    .build());
        });
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent e) {
        if (!firstTime(e.getEventId())) return;
        eventLog.record(e.getOrderId(), e);
        orders.findById(e.getOrderId()).ifPresent(o -> reject(o, e.getReason()));
    }

    private void reject(CustomerOrder o, String reason) {
        if (!o.isPending()) {
            log.info("Ignoring rejection for order {} in status {}", o.getId(), o.getStatus());
            return;
        }
        o.reject(reason);
        eventLog.recordAndPublish(o.getId(), OrderRejectedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(o.getId())
                .setCustomerId(o.getCustomerId())
                .setReason(reason)
                .setOccurredAt(System.currentTimeMillis())
                .build());
    }

    private boolean firstTime(String eventId) {
        if (processed.existsById(eventId)) {
            log.debug("Duplicate event {} skipped", eventId);
            return false;
        }
        processed.save(new ProcessedEvent(eventId));
        return true;
    }
}
