package com.zaalima.ecommerce.common;

import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes an event only after the surrounding DB transaction commits, so a rolled-back
 * transaction never leaks an event. (A full transactional outbox would additionally survive a
 * crash between commit and send; see README "Production hardening".)
 */
public class TransactionalKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionalKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionalKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, SpecificRecord event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, key, event);
                }
            });
        } else {
            send(topic, key, event);
        }
    }

    private void send(String topic, String key, SpecificRecord event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to {} (key={})", event.getClass().getSimpleName(), topic, key, ex);
            } else {
                log.info("Published {} to {}-{}@{} (key={})", event.getClass().getSimpleName(), topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), key);
            }
        });
    }
}
