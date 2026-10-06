package com.eventticketing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HoldRequest(
        @NotBlank String section,
        @Min(1) @Max(8) int quantity
) {}