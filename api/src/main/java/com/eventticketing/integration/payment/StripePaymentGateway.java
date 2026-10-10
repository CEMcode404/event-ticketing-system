package com.eventticketing.integration.payment;

import com.eventticketing.entity.Order;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!loadtest")
public class StripePaymentGateway implements PaymentGateway {

    private static final Duration STRIPE_MINIMUM_SESSION_LIFETIME = Duration.ofMinutes(31);

    private final StripeClient stripe;
    private final String frontendUrl;
    private final String webhookSecret;

    public StripePaymentGateway(StripeClient stripe,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.stripe = stripe;
        this.frontendUrl = frontendUrl;
        this.webhookSecret = webhookSecret;
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

    @Override
    public PaymentEvent parseWebhook(String payload, String signatureHeader) {
        Event event = verifiedEvent(payload, signatureHeader);

        return switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = sessionFrom(event);
                if (!"paid".equals(session.getPaymentStatus())) {
                    yield PaymentEvent.ignored();
                }
                String email = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null;
                yield new PaymentEvent(PaymentEvent.Type.CHECKOUT_COMPLETED, session.getId(), session.getPaymentIntent(), email);
            }
            case "checkout.session.expired" -> {
                Session session = sessionFrom(event);
                yield new PaymentEvent(PaymentEvent.Type.CHECKOUT_EXPIRED, session.getId(), null, null);
            }
            default -> PaymentEvent.ignored();
        };
    }

    @Override
    public void refund(String paymentReference) {
        try {
            stripe.v1().refunds().create(RefundCreateParams.builder()
                    .setPaymentIntent(paymentReference)
                    .build());
        } catch (StripeException e) {
            throw new IllegalStateException("Refund failed for payment " + paymentReference, e);
        }
    }

    private Event verifiedEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }
    }

    private static Session sessionFrom(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject object = deserializer.getObject().orElseGet(() -> deserializeAcrossApiVersions(deserializer));
        return (Session) object;
    }

    private static StripeObject deserializeAcrossApiVersions(EventDataObjectDeserializer deserializer) {
        try {
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new IllegalStateException("Could not read Stripe event data", e);
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