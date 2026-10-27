package com.zaalima.ecommerce.payment.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessorTest {

    private final PaymentProcessor processor = new PaymentProcessor(1_000_000L);

    @Test
    void approvesAmountWithinLimit() {
        assertTrue(processor.charge("o-1", 9_999).success());
        assertTrue(processor.charge("o-2", 1_000_000).success());
    }

    @Test
    void declinesAmountAboveLimit() {
        PaymentProcessor.ChargeResult r = processor.charge("o-3", 1_000_001);
        assertFalse(r.success());
        assertNotNull(r.reason());
    }

    @Test
    void declinesNonPositiveAmount() {
        assertFalse(processor.charge("o-4", 0).success());
    }
}
