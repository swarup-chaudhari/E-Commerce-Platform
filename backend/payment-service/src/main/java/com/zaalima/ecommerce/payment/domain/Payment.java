package com.zaalima.ecommerce.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String orderId;

    private String customerId;
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public static Payment of(String orderId, String customerId, long amountCents,
                             PaymentStatus status, String failureReason) {
        Payment p = new Payment();
        p.id = UUID.randomUUID().toString();
        p.orderId = orderId;
        p.customerId = customerId;
        p.amountCents = amountCents;
        p.status = status;
        p.failureReason = failureReason;
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    /** Compensating transaction for a completed payment. */
    public void refund() {
        if (status == PaymentStatus.COMPLETED) {
            status = PaymentStatus.REFUNDED;
            updatedAt = Instant.now();
        }
    }
}
