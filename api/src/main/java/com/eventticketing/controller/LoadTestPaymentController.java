package com.eventticketing.controller;

import com.eventticketing.integration.payment.FakePaymentGateway;
import com.eventticketing.service.OrderFulfillmentService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("loadtest")
@RequestMapping("/api/test/payments")
public class LoadTestPaymentController {

    private final OrderFulfillmentService fulfillmentService;

    public LoadTestPaymentController(OrderFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<Void> completePayment(@PathVariable UUID orderId) {
        String sessionId = FakePaymentGateway.sessionIdFor(orderId);
        fulfillmentService.completePayment(sessionId, "fake_pi_" + orderId, "loadtest@example.com");
        return ResponseEntity.noContent().build();
    }
}