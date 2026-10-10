package com.eventticketing.service;

import com.eventticketing.IntegrationTestBase;
import com.eventticketing.entity.Order;
import com.eventticketing.entity.Seat;
import com.eventticketing.enums.OrderStatus;
import com.eventticketing.enums.SeatStatus;
import com.eventticketing.integration.payment.PaymentGateway;
import com.eventticketing.repository.OrderRepository;
import com.eventticketing.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderFulfillmentServiceTest extends IntegrationTestBase {

    @Autowired
    OrderFulfillmentService fulfillmentService;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    OrderRepository orderRepository;

    @MockitoBean
    PaymentGateway paymentGateway;

    @Test
    void completingTheSamePaymentTwiceSellsOnceAndNeverRefunds() {
        List<String> seatIds = createHeldSeats("hold-1", Duration.ofMinutes(5));
        Order order = savePendingOrder(seatIds, "hold-1", "cs_test_twice");

        fulfillmentService.completePayment("cs_test_twice", "pi_twice", "buyer@example.com");
        fulfillmentService.completePayment("cs_test_twice", "pi_twice", "buyer@example.com");

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        for (String seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId).orElseThrow();
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD.name());
            assertThat(seat.getOrderId()).isEqualTo(order.getId().toString());
        }
        verify(paymentGateway, never()).refund(anyString());
    }

    @Test
    void paymentForSeatsTakenAfterTheHoldExpiredIsRefunded() throws InterruptedException {
        List<String> seatIds = createHeldSeats("expired-hold", Duration.ofMillis(1));
        Order order = savePendingOrder(seatIds, "expired-hold", "cs_test_lost");

        Thread.sleep(50);
        seatRepository.tryHoldBlock(seatIds, "new-buyer", Duration.ofMinutes(5));

        fulfillmentService.completePayment("cs_test_lost", "pi_lost", "late@example.com");

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(paymentGateway).refund("pi_lost");
        for (String seatId : seatIds) {
            assertThat(seatRepository.findById(seatId).orElseThrow().getHoldToken()).isEqualTo("new-buyer");
        }
    }

    private List<String> createHeldSeats(String holdToken, Duration holdDuration) {
        UUID eventId = UUID.randomUUID();
        List<Seat> seats = IntStream.rangeClosed(1, 2)
                .mapToObj(n -> Seat.newAvailable(eventId, "A", "1", n, 10_000))
                .toList();
        seatRepository.saveAll(seats);
        List<String> seatIds = seats.stream().map(Seat::getId).toList();
        seatRepository.tryHoldBlock(seatIds, holdToken, holdDuration);
        return seatIds;
    }

    private Order savePendingOrder(List<String> seatIds, String holdToken, String sessionId) {
        Order order = Order.pending(UUID.randomUUID(), "A", seatIds, holdToken, "admission-" + holdToken, 20_000);
        order.attachStripeSession(sessionId);
        return orderRepository.save(order);
    }
}