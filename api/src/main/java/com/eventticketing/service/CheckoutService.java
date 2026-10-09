package com.eventticketing.service;

import com.eventticketing.dto.CheckoutRequest;
import com.eventticketing.dto.CheckoutResponse;
import com.eventticketing.entity.Order;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.OrderStatus;
import com.eventticketing.enums.SeatStatus;
import com.eventticketing.integration.payment.CheckoutLineItem;
import com.eventticketing.integration.payment.CheckoutSession;
import com.eventticketing.integration.payment.PaymentGateway;
import com.eventticketing.repository.OrderRepository;
import com.eventticketing.repository.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckoutService {

    private final WaitingRoomService waitingRoomService;
    private final SeatRepository seatRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public CheckoutService(WaitingRoomService waitingRoomService, SeatRepository seatRepository,
                           OrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.waitingRoomService = waitingRoomService;
        this.seatRepository = seatRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    public CheckoutResponse startCheckout(UUID eventId, String admissionToken, CheckoutRequest request) {
        requireAdmission(eventId, admissionToken);
        requireHoldOwnership(eventId, admissionToken, request.holdToken());

        Optional<CheckoutResponse> alreadyStarted = resumeExistingCheckout(request.holdToken());
        if (alreadyStarted.isPresent()) {
            return alreadyStarted.get();
        }

        List<Seat> heldSeats = loadSeatsHeldBy(eventId, request.holdToken(), request.seatIds());
        int totalCents = heldSeats.stream().mapToInt(Seat::getPriceCents).sum();

        Order order = orderRepository.save(Order.pending(
                eventId, heldSeats.getFirst().getSection(), request.seatIds(),
                request.holdToken(), admissionToken, totalCents));

        CheckoutSession session = createPaymentSessionOrExpire(order, heldSeats);
        order.attachStripeSession(session.sessionId());
        orderRepository.save(order);

        return new CheckoutResponse(order.getId(), session.url());
    }

    private void requireAdmission(UUID eventId, String admissionToken) {
        if (waitingRoomService.admissionTimeLeft(eventId, admissionToken).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not admitted, or admission expired");
        }
    }

    private void requireHoldOwnership(UUID eventId, String admissionToken, String holdToken) {
        if (!waitingRoomService.ownsHold(eventId, admissionToken, holdToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This hold does not belong to you");
        }
    }

    private Optional<CheckoutResponse> resumeExistingCheckout(String holdToken) {
        return orderRepository.findFirstByHoldTokenAndStatus(holdToken, OrderStatus.PENDING)
                .filter(order -> order.getStripeSessionId() != null)
                .flatMap(order -> paymentGateway.openCheckoutUrl(order.getStripeSessionId())
                        .map(url -> new CheckoutResponse(order.getId(), url)));
    }

    private List<Seat> loadSeatsHeldBy(UUID eventId, String holdToken, List<String> seatIds) {
        Instant now = Instant.now();
        List<Seat> seats = seatIds.stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found")))
                .toList();

        boolean allStillHeld = seats.stream().allMatch(seat -> isHeldBy(seat, eventId, holdToken, now));
        if (!allStillHeld) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your hold has expired. Please choose seats again.");
        }
        return seats;
    }

    private static boolean isHeldBy(Seat seat, UUID eventId, String holdToken, Instant now) {
        return eventId.toString().equals(seat.getEventId())
                && SeatStatus.AVAILABLE.name().equals(seat.getStatus())
                && holdToken.equals(seat.getHoldToken())
                && seat.getHeldUntil() != null
                && Instant.parse(seat.getHeldUntil()).isAfter(now);
    }

    private CheckoutSession createPaymentSessionOrExpire(Order order, List<Seat> seats) {
        try {
            return paymentGateway.createCheckout(order, toLineItems(seats));
        } catch (RuntimeException e) {
            order.markExpired();
            orderRepository.save(order);
            throw e;
        }
    }

    private static List<CheckoutLineItem> toLineItems(List<Seat> seats) {
        return seats.stream()
                .map(seat -> new CheckoutLineItem(
                        seat.getSection() + " · Row " + seat.getRowLabel() + " · Seat " + seat.getSeatNumber(),
                        seat.getPriceCents()))
                .toList();
    }
}