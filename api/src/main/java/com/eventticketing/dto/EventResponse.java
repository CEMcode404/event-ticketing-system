package com.eventticketing.dto;

import com.eventticketing.entity.Event;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        Instant saleOpensAt,
        Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getSaleOpensAt(),
                event.getCreatedAt()
        );
    }
}