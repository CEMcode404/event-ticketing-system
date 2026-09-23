package com.eventticketing.service;

import com.eventticketing.dto.BulkGenerateSeatsRequest;
import com.eventticketing.dto.HoldSeatResponse;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.dto.SeatSummaryResponse;
import com.eventticketing.entity.Seat;
import com.eventticketing.repository.EventRepository;
import com.eventticketing.repository.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SeatService {

    private static final int MAX_SEATS_PER_CALL = 10_000;
    private static final Duration INITIAL_HOLD_TTL = Duration.ofMinutes(10);
    private static final int BROWSE_LIMIT = 500;

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    public SeatService(SeatRepository seatRepository, EventRepository eventRepository) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
    }

    public List<SeatResponse> bulkGenerate(BulkGenerateSeatsRequest request) {
        long totalSeats = (long) request.rowCount() * request.seatsPerRow();

        if (totalSeats > MAX_SEATS_PER_CALL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bulk generation is limited to " + MAX_SEATS_PER_CALL + " seats per call, requested " + totalSeats
            );
        }

        if (!eventRepository.existsById(request.eventId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= request.rowCount(); row++) {
            String rowLabel = String.valueOf(row);
            for (int seatNumber = 1; seatNumber <= request.seatsPerRow(); seatNumber++) {
                seats.add(Seat.newAvailable(request.eventId(), request.section(), rowLabel, seatNumber, request.priceCents()));
            }
        }

        List<Seat> saved = seatRepository.saveAll(seats);

        return saved.stream().map(SeatResponse::from).toList();
    }

    public List<SeatSummaryResponse> summary(UUID eventId) {
        List<Seat> seats = seatRepository.findAllByEvent(eventId);

        Map<String, List<Seat>> bySection = new LinkedHashMap<>();
        for (Seat seat : seats) {
            bySection.computeIfAbsent(seat.getSection(), s -> new ArrayList<>()).add(seat);
        }

        List<SeatSummaryResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<Seat>> entry : bySection.entrySet()) {
            Integer priceCents = entry.getValue().get(0).getPriceCents();
            result.add(new SeatSummaryResponse(entry.getKey(), entry.getValue().size(), priceCents));
        }
        return result;
    }

    public List<SeatResponse> browse(UUID eventId) {
        return seatRepository.findAvailableByEvent(eventId, BROWSE_LIMIT).stream()
                .map(SeatResponse::from)
                .toList();
    }

    public HoldSeatResponse hold(UUID seatId) {
        String holdToken = UUID.randomUUID().toString();
        boolean acquired = seatRepository.tryAcquireHold(seatId.toString(), holdToken, INITIAL_HOLD_TTL);

        if (!acquired) {
            Optional<Seat> seat = seatRepository.findById(seatId.toString());
            if (seat.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is currently held or no longer available");
        }

        return new HoldSeatResponse(seatId, holdToken, Instant.now().plus(INITIAL_HOLD_TTL));
    }
}