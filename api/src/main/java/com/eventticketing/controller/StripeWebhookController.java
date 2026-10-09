package com.eventticketing.controller;

import com.eventticketing.integration.payment.PaymentEvent;
import com.eventticketing.integration.payment.PaymentGateway;
import com.eventticketing.service.OrderFulfillmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private final PaymentGateway paymentGateway;
    private final OrderFulfillmentService fulfillmentService;

    public StripeWebhookController(PaymentGateway paymentGateway, OrderFulfillmentService fulfillmentService) {
        this.paymentGateway = paymentGateway;
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        PaymentEvent event = paymentGateway.parseWebhook(payload, signature);

        switch (event.type()) {
            case CHECKOUT_COMPLETED -> fulfillmentService.completePayment(
                    event.sessionId(), event.paymentReference(), event.customerEmail());
            case CHECKOUT_EXPIRED -> fulfillmentService.expireCheckout(event.sessionId());
            case IGNORED -> { }
        }
        return ResponseEntity.ok().build();
    }
}