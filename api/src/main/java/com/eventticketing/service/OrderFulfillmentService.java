package com.eventticketing.service;

import com.eventticketing.entity.Order;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.SeatStatus;
import com.eventticketing.integration.payment.PaymentGateway;
import com.eventticketing.repository.OrderRepository;
import com.eventticketing.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentService.class);

    private final OrderRepository orderRepository;
    private final SeatRepository seatRepository;
    private final PaymentGateway paymentGateway;
    private final WaitingRoomService waitingRoomService;

    public OrderFulfillmentService(OrderRepository orderRepository, SeatRepository seatRepository,
                                   PaymentGateway paymentGateway, WaitingRoomService waitingRoomService) {
        this.orderRepository = orderRepository;
        this.seatRepository = seatRepository;
        this.paymentGateway = paymentGateway;
        this.waitingRoomService = waitingRoomService;
    }

    @Transactional
    public void completePayment(String sessionId, String paymentReference, String customerEmail) {
        Optional<Order> maybeOrder = orderRepository.lockByStripeSessionId(sessionId);
        if (maybeOrder.isEmpty()) {
            log.warn("Payment completed for unknown checkout session {}", sessionId);
            return;
        }

        Order order = maybeOrder.get();
        if (!order.isPending()) {
            return;
        }

        String orderId = order.getId().toString();
        boolean sold = seatRepository.trySellBlock(order.getSeatIds(), order.getHoldToken(), orderId)
                || allSeatsAlreadySoldTo(order);

        if (sold) {
            order.markPaid(customerEmail);
        } else {
            paymentGateway.refund(paymentReference);
            order.markRefunded(customerEmail);
            log.info("Refunded order {}: its seats were taken after the hold expired", orderId);
        }

        orderRepository.save(order);
        waitingRoomService.completeShopping(order.getEventId(), order.getAdmissionToken());
    }

    @Transactional
    public void expireCheckout(String sessionId) {
        orderRepository.lockByStripeSessionId(sessionId)
                .filter(Order::isPending)
                .ifPresent(order -> {
                    order.markExpired();
                    orderRepository.save(order);
                });
    }

    private boolean allSeatsAlreadySoldTo(Order order) {
        String orderId = order.getId().toString();
        return order.getSeatIds().stream()
                .map(seatRepository::findById)
                .allMatch(seat -> seat.map(s -> isSoldTo(s, orderId)).orElse(false));
    }

    private static boolean isSoldTo(Seat seat, String orderId) {
        return SeatStatus.SOLD.name().equals(seat.getStatus()) && orderId.equals(seat.getOrderId());
    }
}