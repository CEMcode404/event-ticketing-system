package com.eventticketing.dto;

import java.time.Instant;
import java.util.List;

public record HoldResponse(
        String holdToken,
        String section,
        List<SeatResponse> seats,
        int totalCents,
        Instant expiresAt
) {}