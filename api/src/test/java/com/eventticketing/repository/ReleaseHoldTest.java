package com.eventticketing.repository;

import com.eventticketing.IntegrationTestBase;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseHoldTest extends IntegrationTestBase {

    @Autowired
    SeatRepository seatRepository;

    @Test
    void releasingKeepsTheSeatIntactAndOnlyRemovesTheHold() {
        UUID eventId = UUID.randomUUID();
        Seat seat = Seat.newAvailable(eventId, "VIP", "3", 7, 30_000);
        seatRepository.saveAll(List.of(seat));
        seatRepository.tryHoldBlock(List.of(seat.getId()), "owner", Duration.ofMinutes(5));

        seatRepository.releaseHold(seat.getId(), "owner");

        Seat released = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(released.getHoldToken()).isNull();
        assertThat(released.getHeldUntil()).isNull();
        assertThat(released.getEventId()).isEqualTo(eventId.toString());
        assertThat(released.getSection()).isEqualTo("VIP");
        assertThat(released.getRowLabel()).isEqualTo("3");
        assertThat(released.getSeatNumber()).isEqualTo(7);
        assertThat(released.getPriceCents()).isEqualTo(30_000);
        assertThat(released.getStatus()).isEqualTo(SeatStatus.AVAILABLE.name());
    }

    @Test
    void releasingWithSomeoneElsesTokenLeavesTheirHoldInPlace() {
        Seat seat = Seat.newAvailable(UUID.randomUUID(), "VIP", "1", 1, 30_000);
        seatRepository.saveAll(List.of(seat));
        seatRepository.tryHoldBlock(List.of(seat.getId()), "real-owner", Duration.ofMinutes(5));

        seatRepository.releaseHold(seat.getId(), "someone-else");

        Seat stillHeld = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(stillHeld.getHoldToken()).isEqualTo("real-owner");
        assertThat(stillHeld.getHeldUntil()).isNotNull();
    }
}