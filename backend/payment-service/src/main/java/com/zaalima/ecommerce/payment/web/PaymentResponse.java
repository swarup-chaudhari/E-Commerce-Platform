package com.zaalima.ecommerce.payment.web;

import com.zaalima.ecommerce.payment.domain.Payment;
import com.zaalima.ecommerce.payment.domain.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(String id, String orderId, long amountCents, PaymentStatus status,
                              String failureReason, Instant createdAt, Instant updatedAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmountCents(), p.getStatus(),
                p.getFailureReason(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
