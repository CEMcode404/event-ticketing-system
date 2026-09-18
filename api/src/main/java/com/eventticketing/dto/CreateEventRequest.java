package com.eventticketing.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateEventRequest(
        @NotBlank String name,
        @NotBlank String venue,
        String description,
        @NotNull @Future Instant saleOpensAt
) {
}