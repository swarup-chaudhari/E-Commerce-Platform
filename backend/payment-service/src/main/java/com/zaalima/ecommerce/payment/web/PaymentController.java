package com.zaalima.ecommerce.payment.web;

import com.zaalima.ecommerce.payment.domain.Payment;
import com.zaalima.ecommerce.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> byOrder(@PathVariable String orderId,
                                                   @RequestHeader("X-User-Id") String userId,
                                                   @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return service.findByOrderId(orderId)
                .map(p -> authorize(p, userId, roles))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<PaymentResponse> authorize(Payment p, String userId, String roles) {
        boolean admin = roles.contains("ROLE_ADMIN");
        if (!admin && !userId.equals(p.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(PaymentResponse.from(p));
    }
}
