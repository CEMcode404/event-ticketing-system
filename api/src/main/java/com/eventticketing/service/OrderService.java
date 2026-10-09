package com.eventticketing.service;

import com.eventticketing.dto.OrderResponse;
import com.eventticketing.dto.OrderSeatResponse;
import com.eventticketing.entity.Order;
import com.eventticketing.repository.OrderRepository;
import com.eventticketing.repository.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SeatRepository seatRepository;

    public OrderService(OrderRepository orderRepository, SeatRepository seatRepository) {
        this.orderRepository = orderRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<OrderSeatResponse> seats = order.getSeatIds().stream()
                .map(seatRepository::findById)
                .flatMap(Optional::stream)
                .map(seat -> new OrderSeatResponse(seat.getRowLabel(), seat.getSeatNumber(), seat.getPriceCents()))
                .sorted(Comparator.comparing(OrderSeatResponse::rowLabel).thenComparingInt(OrderSeatResponse::seatNumber))
                .toList();

        return new OrderResponse(order.getId(), order.getEventId(), order.getStatus(),
                order.getSection(), seats, order.getTotalCents());
    }
}