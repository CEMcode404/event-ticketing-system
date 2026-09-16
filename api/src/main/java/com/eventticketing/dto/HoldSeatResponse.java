package com.eventticketing.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * There's no customer login (deliberately scoped out — see architecture
 * doc), so holdToken is what stands in for "prove you're the one who
 * made this hold" at checkout time. Generated fresh per hold, returned
 * once here, and must be sent back by the client on the checkout
 * confirm call later.
 */
public record HoldSeatResponse(
        UUID seatId,
        String holdToken,
        Instant expiresAt
) {
}