package com.zaalima.ecommerce.payment.service;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.common.TransactionalKafkaPublisher;
import com.zaalima.ecommerce.events.InventoryReservedEvent;
import com.zaalima.ecommerce.events.OrderCancelledEvent;
import com.zaalima.ecommerce.events.PaymentCompletedEvent;
import com.zaalima.ecommerce.events.PaymentFailedEvent;
import com.zaalima.ecommerce.payment.domain.Payment;
import com.zaalima.ecommerce.payment.domain.PaymentStatus;
import com.zaalima.ecommerce.payment.domain.ProcessedEvent;
import com.zaalima.ecommerce.payment.repository.PaymentRepository;
import com.zaalima.ecommerce.payment.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ProcessedEventRepository processed;
    private final PaymentProcessor processor;
    private final TransactionalKafkaPublisher publisher;

    public PaymentService(PaymentRepository payments, ProcessedEventRepository processed,
                          PaymentProcessor processor, TransactionalKafkaPublisher publisher) {
        this.payments = payments;
        this.processed = processed;
        this.processor = processor;
        this.publisher = publisher;
    }

    // ---------------- saga step 2: charge ----------------

    @Transactional
    public void charge(InventoryReservedEvent event) {
        if (!firstTime(event.getEventId())) return;
        String orderId = event.getOrderId();
        if (payments.findByOrderId(orderId).isPresent()) {
            log.info("Payment record for order {} already exists (duplicate or cancelled) - skipping charge", orderId);
            return;
        }

        PaymentProcessor.ChargeResult result = processor.charge(orderId, event.getTotalAmountCents());
        if (result.success()) {
            Payment payment = payments.save(Payment.of(orderId, event.getCustomerId(),
                    event.getTotalAmountCents(), PaymentStatus.COMPLETED, null));
            publisher.publish(Topics.PAYMENT_EVENTS, orderId, PaymentCompletedEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setOrderId(orderId)
                    .setPaymentId(payment.getId())
                    .setAmountCents(payment.getAmountCents())
                    .setOccurredAt(System.currentTimeMillis())
                    .build());
            log.info("Payment completed for order {}", orderId);
        } else {
            payments.save(Payment.of(orderId, event.getCustomerId(), event.getTotalAmountCents(),
                    PaymentStatus.FAILED, result.reason()));
            publisher.publish(Topics.PAYMENT_EVENTS, orderId, PaymentFailedEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setOrderId(orderId)
                    .setReason(result.reason())
                    .setOccurredAt(System.currentTimeMillis())
                    .build());
            log.warn("Payment failed for order {}: {}", orderId, result.reason());
        }
    }

    // ---------------- compensating transaction: refund ----------------

    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (!firstTime(event.getEventId())) return;
        String orderId = event.getOrderId();
        Optional<Payment> existing = payments.findByOrderId(orderId);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                payment.refund();
                log.info("Refunded payment {} for cancelled order {}", payment.getId(), orderId);
            }
        } else {
            // Cancelled before we charged: leave a tombstone so a late InventoryReserved never charges.
            payments.save(Payment.of(orderId, null, 0, PaymentStatus.CANCELLED, "Order cancelled before payment"));
            log.info("Recorded cancellation tombstone for order {}", orderId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(String orderId) {
        return payments.findByOrderId(orderId);
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
