package com.eventticketing.integration.payment;

public record PaymentEvent(Type type, String sessionId, String paymentReference, String customerEmail) {

    public enum Type {
        CHECKOUT_COMPLETED,
        CHECKOUT_EXPIRED,
        IGNORED
    }

    public static PaymentEvent ignored() {
        return new PaymentEvent(Type.IGNORED, null, null, null);
    }
}