package com.eventticketing.dto;

import com.eventticketing.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID eventId,
        OrderStatus status,
        String section,
        List<OrderSeatResponse> seats,
        int totalCents
) {}