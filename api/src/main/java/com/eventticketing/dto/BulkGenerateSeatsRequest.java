package com.eventticketing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BulkGenerateSeatsRequest(
        @NotNull UUID eventId,
        @NotBlank String section,
        @Min(1) int rowCount,
        @Min(1) int seatsPerRow,
        @Min(0) int priceCents
) {
}