package com.eventticketing.dto;

import com.eventticketing.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusRequest(
        @NotNull EventStatus status
) {
}