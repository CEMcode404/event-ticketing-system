package com.eventticketing.dto;

import java.util.UUID;

public record CheckoutResponse(UUID orderId, String checkoutUrl) {}