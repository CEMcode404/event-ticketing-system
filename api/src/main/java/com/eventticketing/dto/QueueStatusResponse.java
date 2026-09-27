package com.eventticketing.dto;

import java.time.Instant;

public record QueueStatusResponse(
        Status status,
        Long position,
        String admissionToken,
        Instant expiresAt
) {
    public enum Status { WAITING, ADMITTED }

    public static QueueStatusResponse waiting(long position) {
        return new QueueStatusResponse(Status.WAITING, position, null, null);
    }

    public static QueueStatusResponse admitted(String token, Instant expiresAt) {
        return new QueueStatusResponse(Status.ADMITTED, null, token, expiresAt);
    }
}