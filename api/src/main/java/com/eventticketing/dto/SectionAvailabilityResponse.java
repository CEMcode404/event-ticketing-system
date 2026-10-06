package com.eventticketing.dto;

public record SectionAvailabilityResponse(
        String section,
        int available,
        Integer priceCents
) {}