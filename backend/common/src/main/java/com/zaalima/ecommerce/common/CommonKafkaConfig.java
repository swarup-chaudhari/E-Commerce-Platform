package com.zaalima.ecommerce.common;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Import this from each service's @SpringBootApplication class. */
@Configuration
public class CommonKafkaConfig {

    @Bean
    TransactionalKafkaPublisher transactionalKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new TransactionalKafkaPublisher(kafkaTemplate);
    }

    /** Retry failed handlers 5x (1s apart) before the record is logged and skipped. Deserialization errors are never retried. */
    @Bean
    CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 5L));
    }

    @Bean
    NewTopic orderEventsTopic(@Value("${app.kafka.partitions:6}") int partitions,
                              @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic inventoryEventsTopic(@Value("${app.kafka.partitions:6}") int partitions,
                                  @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic paymentEventsTopic(@Value("${app.kafka.partitions:6}") int partitions,
                                @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.PAYMENT_EVENTS).partitions(partitions).replicas(replicas).build();
    }
}
