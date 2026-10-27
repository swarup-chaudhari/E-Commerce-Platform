package com.zaalima.ecommerce.order.service;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.common.TransactionalKafkaPublisher;
import com.zaalima.ecommerce.order.domain.OrderEventRecord;
import com.zaalima.ecommerce.order.repository.OrderEventRepository;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Event-sourcing helper: every business fact about an order is appended to the event store. */
@Component
public class OrderEventLog {

    private final OrderEventRepository repository;
    private final TransactionalKafkaPublisher publisher;

    public OrderEventLog(OrderEventRepository repository, TransactionalKafkaPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Append an event received from another service. */
    public void record(String orderId, SpecificRecord event) {
        repository.save(new OrderEventRecord(orderId, event.getClass().getSimpleName(),
                event.toString(), Instant.now()));
    }

    /** Append an event this service originates and publish it (after commit) keyed by orderId. */
    public void recordAndPublish(String orderId, SpecificRecord event) {
        record(orderId, event);
        publisher.publish(Topics.ORDER_EVENTS, orderId, event);
    }
}
