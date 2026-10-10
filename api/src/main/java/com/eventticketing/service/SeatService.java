package com.eventticketing.service;

import com.eventticketing.dto.BulkGenerateSeatsRequest;
import com.eventticketing.dto.HoldRequest;
import com.eventticketing.dto.HoldResponse;
import com.eventticketing.dto.SeatResponse;
import com.eventticketing.dto.SeatSummaryResponse;
import com.eventticketing.dto.SectionAvailabilityResponse;
import com.eventticketing.entity.Event;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import com.eventticketing.repository.EventRepository;
import com.eventticketing.repository.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SeatService {

    private static final int MAX_SEATS_PER_CALL = 10_000;
    private static final int MAX_SEATS_READ_PER_SECTION = 10_000;
    private static final int SECTION_READ_ROUNDS = 3;
    private static final int BLOCK_ATTEMPTS_PER_READ = 5;

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final WaitingRoomService waitingRoomService;

    public SeatService(SeatRepository seatRepository, EventRepository eventRepository, WaitingRoomService waitingRoomService) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.waitingRoomService = waitingRoomService;
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
        if (event.hasStarted(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket sales for this event have closed");
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

    public List<SectionAvailabilityResponse> sections(UUID eventId, String admissionToken) {
        requireAdmission(eventId, admissionToken);

        Instant now = Instant.now();
        Map<String, Integer> availableBySection = new TreeMap<>();
        Map<String, Integer> priceBySection = new HashMap<>();

        for (Seat seat : seatRepository.findAllByEvent(eventId)) {
            availableBySection.putIfAbsent(seat.getSection(), 0);
            priceBySection.putIfAbsent(seat.getSection(), seat.getPriceCents());
            if (isHoldable(seat, now)) {
                availableBySection.merge(seat.getSection(), 1, Integer::sum);
            }
        }

        return availableBySection.entrySet().stream()
                .map(e -> new SectionAvailabilityResponse(e.getKey(), e.getValue(), priceBySection.get(e.getKey())))
                .toList();
    }

    public HoldResponse holdSeats(UUID eventId, String admissionToken, HoldRequest request) {
        Duration admissionTimeLeft = requireAdmission(eventId, admissionToken);

        String holdToken = UUID.randomUUID().toString();
        if (!waitingRoomService.claimHoldSlot(eventId, admissionToken, holdToken, admissionTimeLeft)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have seats on hold");
        }

        List<Seat> heldSeats = holdRandomBlockOrReleaseSlot(eventId, admissionToken, request, holdToken, admissionTimeLeft);

        int totalCents = heldSeats.stream().mapToInt(Seat::getPriceCents).sum();
        return new HoldResponse(
                holdToken,
                request.section(),
                heldSeats.stream().map(SeatResponse::from).toList(),
                totalCents,
                Instant.now().plus(admissionTimeLeft)
        );
    }

    private List<Seat> holdRandomBlockOrReleaseSlot(UUID eventId, String admissionToken, HoldRequest request,
                                                    String holdToken, Duration holdDuration) {
        List<Seat> heldSeats;
        try {
            heldSeats = holdRandomBlock(eventId, request.section(), request.quantity(), holdToken, holdDuration);
        } catch (RuntimeException e) {
            waitingRoomService.releaseHoldSlot(eventId, admissionToken);
            throw e;
        }

        if (heldSeats.isEmpty()) {
            waitingRoomService.releaseHoldSlot(eventId, admissionToken);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No " + request.quantity() + " seats together in section " + request.section()
                            + ". Try fewer tickets or another section.");
        }
        return heldSeats;
    }

    private List<Seat> holdRandomBlock(UUID eventId, String section, int quantity, String holdToken, Duration holdDuration) {
        for (int round = 0; round < SECTION_READ_ROUNDS; round++) {
            List<Seat> holdableSeats = seatRepository.findAvailableInSection(eventId, section, MAX_SEATS_READ_PER_SECTION);
            List<List<Seat>> adjacentBlocks = findAdjacentBlocks(holdableSeats, quantity);
            if (adjacentBlocks.isEmpty()) {
                return List.of();
            }

            Collections.shuffle(adjacentBlocks);
            int attempts = Math.min(BLOCK_ATTEMPTS_PER_READ, adjacentBlocks.size());
            for (List<Seat> block : adjacentBlocks.subList(0, attempts)) {
                List<String> seatIds = block.stream().map(Seat::getId).toList();
                if (seatRepository.tryHoldBlock(seatIds, holdToken, holdDuration)) {
                    return block;
                }
            }
        }
        return List.of();
    }

    private static List<List<Seat>> findAdjacentBlocks(List<Seat> seats, int quantity) {
        Map<String, List<Seat>> seatsByRow = new HashMap<>();
        for (Seat seat : seats) {
            seatsByRow.computeIfAbsent(seat.getRowLabel(), r -> new ArrayList<>()).add(seat);
        }

        List<List<Seat>> blocks = new ArrayList<>();
        for (List<Seat> row : seatsByRow.values()) {
            row.sort(Comparator.comparing(Seat::getSeatNumber));
            for (int start = 0; start + quantity <= row.size(); start++) {
                List<Seat> window = row.subList(start, start + quantity);
                if (isConsecutive(window)) {
                    blocks.add(List.copyOf(window));
                }
            }
        }
        return blocks;
    }

    private static boolean isConsecutive(List<Seat> sortedSeats) {
        int firstNumber = sortedSeats.getFirst().getSeatNumber();
        int lastNumber = sortedSeats.getLast().getSeatNumber();
        return lastNumber - firstNumber == sortedSeats.size() - 1;
    }

    private Duration requireAdmission(UUID eventId, String admissionToken) {
        return waitingRoomService.admissionTimeLeft(eventId, admissionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not admitted, or admission expired"));
    }

    private static boolean isHoldable(Seat seat, Instant now) {
        boolean available = SeatStatus.AVAILABLE.name().equals(seat.getStatus());
        boolean notHeld = seat.getHeldUntil() == null || Instant.parse(seat.getHeldUntil()).isBefore(now);
        return available && notHeld;
    }
}