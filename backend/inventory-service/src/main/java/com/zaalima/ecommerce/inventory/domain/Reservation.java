package com.zaalima.ecommerce.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One reservation per order. Its existence makes reserve() idempotent; its status drives compensation. */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor
public class Reservation {

    @Id
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<ReservedItem> items = new ArrayList<>();

    public Reservation(String orderId, List<ReservedItem> items) {
        this.orderId = orderId;
        this.items = new ArrayList<>(items);
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
    }
}
