package com.eventticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotBlank String holdToken,
        @NotEmpty @Size(max = 8) List<String> seatIds
) {}