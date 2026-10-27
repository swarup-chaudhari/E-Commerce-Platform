package com.zaalima.ecommerce.order.service;

import com.zaalima.ecommerce.events.OrderCancelledEvent;
import com.zaalima.ecommerce.events.OrderCreatedEvent;
import com.zaalima.ecommerce.events.OrderLineItem;
import com.zaalima.ecommerce.order.client.AvailabilityResult;
import com.zaalima.ecommerce.order.client.InventoryClient;
import com.zaalima.ecommerce.order.domain.CustomerOrder;
import com.zaalima.ecommerce.order.domain.OrderEventRecord;
import com.zaalima.ecommerce.order.domain.OrderItem;
import com.zaalima.ecommerce.order.repository.OrderEventRepository;
import com.zaalima.ecommerce.order.repository.OrderRepository;
import com.zaalima.ecommerce.order.web.CreateOrderRequest;
import com.zaalima.ecommerce.order.web.errors.OrderAccessDeniedException;
import com.zaalima.ecommerce.order.web.errors.OrderNotFoundException;
import com.zaalima.ecommerce.order.web.errors.OutOfStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderEventRepository eventRepository;
    private final OrderEventLog eventLog;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate tx;

    public OrderService(OrderRepository orders, OrderEventRepository eventRepository, OrderEventLog eventLog,
                        InventoryClient inventoryClient, PlatformTransactionManager transactionManager) {
        this.orders = orders;
        this.eventRepository = eventRepository;
        this.eventLog = eventLog;
        this.inventoryClient = inventoryClient;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Not @Transactional on purpose: the remote pre-check must not hold a DB connection. */
    public CustomerOrder createOrder(String customerId, CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPriceCents()))
                .toList();

        AvailabilityResult availability = inventoryClient.checkAvailability(items);
        if (availability.checked() && !availability.unavailableProductIds().isEmpty()) {
            throw new OutOfStockException(availability.unavailableProductIds());
        }

        return tx.execute(status -> {
            CustomerOrder order = CustomerOrder.create(customerId, items);
            orders.save(order);
            eventLog.recordAndPublish(order.getId(), toCreatedEvent(order));
            return order;
        });
    }

    @Transactional(readOnly = true)
    public CustomerOrder getOrder(String id, String userId, String roles) {
        return getAuthorized(id, userId, roles);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> listMyOrders(String userId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<OrderEventRecord> history(String id, String userId, String roles) {
        getAuthorized(id, userId, roles);
        return eventRepository.findByOrderIdOrderByIdAsc(id);
    }

    @Transactional
    public CustomerOrder cancel(String id, String userId, String roles) {
        CustomerOrder order = getAuthorized(id, userId, roles);
        order.cancel("Cancelled by customer");
        eventLog.recordAndPublish(id, OrderCancelledEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(id)
                .setReason("Cancelled by customer")
                .setOccurredAt(System.currentTimeMillis())
                .build());
        return order;
    }

    private CustomerOrder getAuthorized(String id, String userId, String roles) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        boolean admin = roles != null && roles.contains("ROLE_ADMIN");
        if (!admin && !order.getCustomerId().equals(userId)) {
            throw new OrderAccessDeniedException(id);
        }
        return order;
    }

    private OrderCreatedEvent toCreatedEvent(CustomerOrder order) {
        return OrderCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(order.getId())
                .setCustomerId(order.getCustomerId())
                .setItems(order.getItems().stream()
                        .map(i -> OrderLineItem.newBuilder()
                                .setProductId(i.getProductId())
                                .setQuantity(i.getQuantity())
                                .setUnitPriceCents(i.getUnitPriceCents())
                                .build())
                        .toList())
                .setTotalAmountCents(order.getTotalAmountCents())
                .setOccurredAt(System.currentTimeMillis())
                .build();
    }
}
