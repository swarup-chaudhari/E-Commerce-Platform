package com.zaalima.ecommerce.inventory.service;

import com.zaalima.ecommerce.common.Topics;
import com.zaalima.ecommerce.common.TransactionalKafkaPublisher;
import com.zaalima.ecommerce.events.InventoryReservationFailedEvent;
import com.zaalima.ecommerce.events.InventoryReservedEvent;
import com.zaalima.ecommerce.events.OrderCreatedEvent;
import com.zaalima.ecommerce.inventory.domain.ProcessedEvent;
import com.zaalima.ecommerce.inventory.domain.Reservation;
import com.zaalima.ecommerce.inventory.domain.ReservationStatus;
import com.zaalima.ecommerce.inventory.domain.ReservedItem;
import com.zaalima.ecommerce.inventory.domain.StockItem;
import com.zaalima.ecommerce.inventory.repository.jpa.ProcessedEventRepository;
import com.zaalima.ecommerce.inventory.repository.jpa.ReservationRepository;
import com.zaalima.ecommerce.inventory.repository.jpa.StockItemRepository;
import com.zaalima.ecommerce.inventory.web.AvailabilityResponse;
import com.zaalima.ecommerce.inventory.web.CheckRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final ProcessedEventRepository processed;
    private final TransactionalKafkaPublisher publisher;

    public InventoryService(StockItemRepository stock, ReservationRepository reservations,
                            ProcessedEventRepository processed, TransactionalKafkaPublisher publisher) {
        this.stock = stock;
        this.reservations = reservations;
        this.processed = processed;
        this.publisher = publisher;
    }

    // ---------------- saga step 1: reserve (deduct) stock ----------------

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (!firstTime(event.getEventId())) return;
        String orderId = event.getOrderId();
        if (reservations.existsById(orderId)) {
            log.info("Reservation for order {} already exists - skipping", orderId);
            return;
        }

        Map<String, Integer> required = new TreeMap<>();
        event.getItems().forEach(i -> required.merge(i.getProductId(), i.getQuantity(), Integer::sum));

        Map<String, StockItem> locked = stock.findByProductIdInOrderByProductId(required.keySet()).stream()
                .collect(Collectors.toMap(StockItem::getProductId, Function.identity()));

        for (Map.Entry<String, Integer> line : required.entrySet()) {
            StockItem item = locked.get(line.getKey());
            if (item == null) {
                fail(orderId, "Unknown product " + line.getKey());
                return;
            }
            if (item.getAvailableQuantity() < line.getValue()) {
                fail(orderId, "Insufficient stock for " + line.getKey());
                return;
            }
        }

        required.forEach((productId, qty) -> {
            StockItem item = locked.get(productId);
            item.setAvailableQuantity(item.getAvailableQuantity() - qty);
        });
        reservations.save(new Reservation(orderId, required.entrySet().stream()
                .map(e -> new ReservedItem(e.getKey(), e.getValue())).toList()));

        publisher.publish(Topics.INVENTORY_EVENTS, orderId, InventoryReservedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setCustomerId(event.getCustomerId())
                .setTotalAmountCents(event.getTotalAmountCents())
                .setOccurredAt(System.currentTimeMillis())
                .build());
        log.info("Reserved stock for order {}", orderId);
    }

    private void fail(String orderId, String reason) {
        log.warn("Stock reservation failed for order {}: {}", orderId, reason);
        publisher.publish(Topics.INVENTORY_EVENTS, orderId, InventoryReservationFailedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setReason(reason)
                .setOccurredAt(System.currentTimeMillis())
                .build());
    }

    // ---------------- compensating transaction: release stock ----------------

    /** Triggered by PaymentFailed or OrderCancelled. Safe to call repeatedly. */
    @Transactional
    public void release(String eventId, String orderId, String why) {
        if (!firstTime(eventId)) return;
        reservations.findById(orderId)
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .ifPresentOrElse(r -> {
                    List<String> ids = r.getItems().stream().map(ReservedItem::getProductId).sorted().toList();
                    Map<String, StockItem> locked = stock.findByProductIdInOrderByProductId(ids).stream()
                            .collect(Collectors.toMap(StockItem::getProductId, Function.identity()));
                    r.getItems().forEach(i -> {
                        StockItem item = locked.get(i.getProductId());
                        if (item != null) item.setAvailableQuantity(item.getAvailableQuantity() + i.getQuantity());
                    });
                    r.release();
                    log.info("Released stock for order {} ({})", orderId, why);
                }, () -> log.info("No active reservation for order {} ({}); nothing to release", orderId, why));
    }

    // ---------------- synchronous API support ----------------

    @Transactional(readOnly = true)
    public AvailabilityResponse check(CheckRequest request) {
        Map<String, Integer> required = new TreeMap<>();
        request.items().forEach(i -> required.merge(i.productId(), i.quantity(), Integer::sum));
        Map<String, Integer> available = stock.findAllById(required.keySet()).stream()
                .collect(Collectors.toMap(StockItem::getProductId, StockItem::getAvailableQuantity));
        List<String> unavailable = required.entrySet().stream()
                .filter(e -> available.getOrDefault(e.getKey(), 0) < e.getValue())
                .map(Map.Entry::getKey)
                .toList();
        return new AvailabilityResponse(true, unavailable);
    }

    @Transactional(readOnly = true)
    public StockItem getStock(String productId) {
        return stock.findById(productId).orElse(null);
    }

    @Transactional
    public StockItem setStock(String productId, int quantity) {
        StockItem item = stock.findById(productId).orElseGet(() -> new StockItem(productId, 0));
        item.setAvailableQuantity(quantity);
        return stock.save(item);
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
