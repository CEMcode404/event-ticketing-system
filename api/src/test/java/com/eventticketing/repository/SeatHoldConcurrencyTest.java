package com.eventticketing.repository;

import com.eventticketing.IntegrationTestBase;
import com.eventticketing.entity.Seat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SeatHoldConcurrencyTest extends IntegrationTestBase {

    private static final int CONTENDERS = 20;

    @Autowired
    SeatRepository seatRepository;

    @Test
    void onlyOneOfManyConcurrentHoldsOnTheSameSeatsSucceeds() throws Exception {
        List<String> seatIds = createSeats(2);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Callable<String>> attempts = IntStream.range(0, CONTENDERS)
                .mapToObj(i -> (Callable<String>) () -> {
                    String holdToken = "contender-" + i;
                    startGate.await();
                    return seatRepository.tryHoldBlock(seatIds, holdToken, Duration.ofMinutes(5)) ? holdToken : null;
                })
                .toList();

        List<String> winners = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS)) {
            List<Future<String>> results = attempts.stream().map(pool::submit).toList();
            startGate.countDown();
            for (Future<String> result : results) {
                String winner = result.get();
                if (winner != null) {
                    winners.add(winner);
                }
            }
        }

        assertThat(winners).hasSize(1);
        String winner = winners.getFirst();
        for (String seatId : seatIds) {
            assertThat(seatRepository.findById(seatId))
                    .hasValueSatisfying(seat -> assertThat(seat.getHoldToken()).isEqualTo(winner));
        }
    }

    private List<String> createSeats(int count) {
        UUID eventId = UUID.randomUUID();
        List<Seat> seats = IntStream.rangeClosed(1, count)
                .mapToObj(n -> Seat.newAvailable(eventId, "A", "1", n, 10_000))
                .toList();
        seatRepository.saveAll(seats);
        return seats.stream().map(Seat::getId).toList();
    }
}