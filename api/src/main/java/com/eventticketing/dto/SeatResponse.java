package com.eventticketing.dto;

import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;

import java.util.UUID;

public record SeatResponse(
        UUID id,
        String section,
        String rowLabel,
        Integer seatNumber,
        Integer priceCents,
        SeatStatus status
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                UUID.fromString(seat.getId()),
                seat.getSection(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getPriceCents(),
                SeatStatus.valueOf(seat.getStatus())
        );
    }
}