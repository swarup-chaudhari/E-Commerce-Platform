package com.zaalima.ecommerce.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real PSP integration (Stripe, Razorpay, ...). Deterministic for demos/tests:
 * any charge above the configured limit is declined.
 */
@Component
public class PaymentProcessor {

    public record ChargeResult(boolean success, String reason) {}

    private final long declineAboveCents;

    public PaymentProcessor(@Value("${app.payment.decline-above-cents:1000000}") long declineAboveCents) {
        this.declineAboveCents = declineAboveCents;
    }

    public ChargeResult charge(String orderId, long amountCents) {
        if (amountCents <= 0) {
            return new ChargeResult(false, "Invalid amount");
        }
        if (amountCents > declineAboveCents) {
            return new ChargeResult(false, "Card declined: amount exceeds limit");
        }
        return new ChargeResult(true, null);
    }
}
