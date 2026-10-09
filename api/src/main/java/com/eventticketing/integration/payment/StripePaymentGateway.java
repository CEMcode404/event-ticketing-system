package com.eventticketing.integration.payment;

import com.eventticketing.entity.Order;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final Duration STRIPE_MINIMUM_SESSION_LIFETIME = Duration.ofMinutes(31);

    private final StripeClient stripe;
    private final String frontendUrl;

    public StripePaymentGateway(StripeClient stripe, @Value("${app.frontend-url}") String frontendUrl) {
        this.stripe = stripe;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public CheckoutSession createCheckout(Order order, List<CheckoutLineItem> lineItems) {
        String orderId = order.getId().toString();
        String eventPath = frontendUrl + "/events/" + order.getEventId();

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(orderId)
                .putMetadata("orderId", orderId)
                .setSuccessUrl(eventPath + "/orders/" + orderId)
                .setCancelUrl(eventPath + "/shop")
                .setExpiresAt(Instant.now().plus(STRIPE_MINIMUM_SESSION_LIFETIME).getEpochSecond());

        lineItems.forEach(item -> params.addLineItem(toStripeLineItem(item)));

        try {
            Session session = stripe.v1().checkout().sessions().create(params.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider unavailable", e);
        }
    }

    @Override
    public Optional<String> openCheckoutUrl(String sessionId) {
        try {
            Session session = stripe.v1().checkout().sessions().retrieve(sessionId);
            boolean stillOpen = "open".equals(session.getStatus());
            return stillOpen ? Optional.ofNullable(session.getUrl()) : Optional.empty();
        } catch (StripeException e) {
            return Optional.empty();
        }
    }

    private static SessionCreateParams.LineItem toStripeLineItem(CheckoutLineItem item) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(item.amountCents())
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(item.name())
                                .build())
                        .build())
                .build();
    }
}