package com.eventticketing.service;

import com.eventticketing.dto.BulkGenerateSeatsRequest;
import com.eventticketing.dto.HoldSeatResponse;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.entity.Event;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import com.eventticketing.repository.EventRepository;
import com.eventticketing.repository.SeatHoldRepository;
import com.eventticketing.repository.SeatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private static final int MAX_SEATS_PER_CALL = 10_000;

    private static final Duration INITIAL_HOLD_TTL = Duration.ofMinutes(10);

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final SeatHoldRepository seatHoldRepository;

    public SeatService(
            SeatRepository seatRepository,
            EventRepository eventRepository,
            SeatHoldRepository seatHoldRepository
    ) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.seatHoldRepository = seatHoldRepository;
    }

    public List<SeatResponse> bulkGenerate(BulkGenerateSeatsRequest request) {
        long totalSeats = (long) request.rowCount() * request.seatsPerRow();

        if (totalSeats > MAX_SEATS_PER_CALL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bulk generation is limited to " + MAX_SEATS_PER_CALL + " seats per call, requested " + totalSeats
            );
        }

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= request.rowCount(); row++) {
            String rowLabel = String.valueOf(row);
            for (int seatNumber = 1; seatNumber <= request.seatsPerRow(); seatNumber++) {
                seats.add(new Seat(event, request.section(), rowLabel, seatNumber, request.priceCents()));
            }
        }

        List<Seat> saved = seatRepository.saveAll(seats);

        return saved.stream().map(SeatResponse::from).toList();
    }

    public Page<SeatResponse> browse(UUID eventId, Pageable pageable) {
        return seatRepository.findByEventIdAndStatus(eventId, SeatStatus.AVAILABLE, pageable)
                .map(SeatResponse::from);
    }

    public HoldSeatResponse hold(UUID seatId) {
        String holdToken = UUID.randomUUID().toString();
        boolean acquired = seatHoldRepository.tryAcquire(seatId, holdToken, INITIAL_HOLD_TTL);

        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is currently held by someone else");
        }

        Seat seat = seatRepository.findById(seatId).orElse(null);

        if (seat == null) {
            seatHoldRepository.release(seatId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found");
        }

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            seatHoldRepository.release(seatId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is no longer available");
        }

        return new HoldSeatResponse(seatId, holdToken, Instant.now().plus(INITIAL_HOLD_TTL));
    }
}