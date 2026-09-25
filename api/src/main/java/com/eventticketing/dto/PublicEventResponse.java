package com.eventticketing.dto;

import com.eventticketing.entity.Event;

import java.time.Instant;
import java.util.UUID;

public record PublicEventResponse(
        UUID id,
        String name,
        String venue,
        String description,
        Instant saleOpensAt
) {
    public static PublicEventResponse from(Event event) {
        return new PublicEventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getDescription(),
                event.getSaleOpensAt()
        );
    }
}