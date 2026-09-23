package com.eventticketing.dto;

public record SeatSummaryResponse(
        String section,
        long count,
        Integer priceCents
) {
}