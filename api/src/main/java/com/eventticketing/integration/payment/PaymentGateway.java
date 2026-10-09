package com.eventticketing.integration.payment;

import com.eventticketing.entity.Order;

import java.util.List;
import java.util.Optional;

public interface PaymentGateway {

    CheckoutSession createCheckout(Order order, List<CheckoutLineItem> lineItems);

    Optional<String> openCheckoutUrl(String sessionId);

    PaymentEvent parseWebhook(String payload, String signatureHeader);

    void refund(String paymentReference);
}