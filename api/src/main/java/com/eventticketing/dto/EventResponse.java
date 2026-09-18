package com.eventticketing.dto;

import com.eventticketing.entity.Event;
import com.eventticketing.enums.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String venue,
        String description,
        Instant saleOpensAt,
        EventStatus status,
        Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getDescription(),
                event.getSaleOpensAt(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }
}