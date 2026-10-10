package com.eventticketing.integration.payment;

import com.eventticketing.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("loadtest")
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);
    private static final String SESSION_PREFIX = "fake_cs_";

    private final String frontendUrl;
    private final Map<String, String> openCheckoutUrls = new ConcurrentHashMap<>();

    public FakePaymentGateway(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public static String sessionIdFor(UUID orderId) {
        return SESSION_PREFIX + orderId;
    }

    @Override
    public CheckoutSession createCheckout(Order order, List<CheckoutLineItem> lineItems) {
        String sessionId = sessionIdFor(order.getId());
        String orderPageUrl = frontendUrl + "/events/" + order.getEventId() + "/orders/" + order.getId();
        openCheckoutUrls.put(sessionId, orderPageUrl);
        return new CheckoutSession(sessionId, orderPageUrl);
    }

    @Override
    public Optional<String> openCheckoutUrl(String sessionId) {
        return Optional.ofNullable(openCheckoutUrls.get(sessionId));
    }

    @Override
    public PaymentEvent parseWebhook(String payload, String signatureHeader) {
        return PaymentEvent.ignored();
    }

    @Override
    public void refund(String paymentReference) {
        log.info("Fake refund for {}", paymentReference);
    }
}