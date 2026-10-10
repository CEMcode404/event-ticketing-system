package com.eventticketing.service;

import com.eventticketing.dto.QueueJoinResponse;
import com.eventticketing.dto.QueueStatusResponse;
import com.eventticketing.entity.Event;
import com.eventticketing.enums.EventStatus;
import com.eventticketing.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class WaitingRoomService {

    private static final String ACTIVE_EVENTS = "wr:events";

    private final StringRedisTemplate redis;
    private final EventRepository eventRepository;
    private final int admitBatchSize;
    private final Duration heartbeatTimeout;
    private final Duration admissionTtl;
    private final int maxActiveShoppers;

    public WaitingRoomService(
            StringRedisTemplate redis,
            EventRepository eventRepository,
            @Value("${waitingroom.admit-batch-size:50}") int admitBatchSize,
            @Value("${waitingroom.heartbeat-timeout-seconds:30}") long heartbeatTimeoutSeconds,
            @Value("${waitingroom.admission-ttl-minutes:10}") long admissionTtlMinutes,
            @Value("${waitingroom.max-active-shoppers:500}") int maxActiveShoppers
    ) {
        this.redis = redis;
        this.eventRepository = eventRepository;
        this.admitBatchSize = admitBatchSize;
        this.heartbeatTimeout = Duration.ofSeconds(heartbeatTimeoutSeconds);
        this.admissionTtl = Duration.ofMinutes(admissionTtlMinutes);
        this.maxActiveShoppers = maxActiveShoppers;
    }

    public QueueJoinResponse join(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not on sale");
        }
        if (event.hasStarted(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket sales have closed");
        }

        String sessionId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        redis.opsForSet().add(ACTIVE_EVENTS, eventId.toString());
        ZSetOperations<String, String> zset = redis.opsForZSet();
        zset.add(heartbeatKey(eventId), sessionId, now);
        zset.add(queueKey(eventId), sessionId, lotteryOrArrivalScore(event, now));

        return new QueueJoinResponse(sessionId, status(eventId, sessionId));
    }

    private static double lotteryOrArrivalScore(Event event, long nowMillis) {
        boolean joinedBeforeSale = Instant.ofEpochMilli(nowMillis).isBefore(event.getSaleOpensAt());
        double randomLotteryScoreBelowAnyTimestamp = ThreadLocalRandom.current().nextDouble();
        return joinedBeforeSale ? randomLotteryScoreBelowAnyTimestamp : nowMillis;
    }

    public QueueStatusResponse status(UUID eventId, String sessionId) {
        String token = redis.opsForValue().get(admittedKey(eventId, sessionId));
        if (token != null) {
            return admittedResponse(eventId, sessionId, token);
        }

        Long rank = redis.opsForZSet().rank(queueKey(eventId), sessionId);
        if (rank == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in queue");
        }

        redis.opsForZSet().add(heartbeatKey(eventId), sessionId, System.currentTimeMillis());
        return QueueStatusResponse.waiting(rank + 1);
    }

    public boolean isAdmitted(UUID eventId, String token) {
        String value = redis.opsForValue().get(tokenKey(token));
        return value != null && value.startsWith(eventId + ":");
    }

    public Optional<Duration> admissionTimeLeft(UUID eventId, String token) {
        if (!isAdmitted(eventId, token)) {
            return Optional.empty();
        }
        Long secondsLeft = redis.getExpire(tokenKey(token));
        if (secondsLeft == null || secondsLeft <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(secondsLeft));
    }

    public boolean claimHoldSlot(UUID eventId, String admissionToken, String holdToken, Duration ttl) {
        Boolean claimed = redis.opsForValue().setIfAbsent(holdKey(eventId, admissionToken), holdToken, ttl);
        return Boolean.TRUE.equals(claimed);
    }

    public boolean ownsHold(UUID eventId, String admissionToken, String holdToken) {
        return holdToken.equals(redis.opsForValue().get(holdKey(eventId, admissionToken)));
    }

    public void releaseHoldSlot(UUID eventId, String admissionToken) {
        redis.delete(holdKey(eventId, admissionToken));
    }

    public void completeShopping(UUID eventId, String admissionToken) {
        String admission = redis.opsForValue().get(tokenKey(admissionToken));
        if (admission != null) {
            String sessionId = admission.substring(admission.indexOf(':') + 1);
            redis.opsForZSet().remove(activeKey(eventId), sessionId);
            redis.delete(admittedKey(eventId, sessionId));
        }
        redis.delete(tokenKey(admissionToken));
        redis.delete(holdKey(eventId, admissionToken));
    }

    public Set<UUID> activeEventIds() {
        Set<String> members = redis.opsForSet().members(ACTIVE_EVENTS);
        if (members == null) {
            return Set.of();
        }
        return members.stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    public void sweepAbandoned(UUID eventId) {
        double lastSeenCutoff = System.currentTimeMillis() - heartbeatTimeout.toMillis();
        Set<String> abandonedSessions = redis.opsForZSet().rangeByScore(heartbeatKey(eventId), 0, lastSeenCutoff);
        if (abandonedSessions == null || abandonedSessions.isEmpty()) {
            return;
        }

        Object[] sessions = abandonedSessions.toArray();
        redis.opsForZSet().remove(queueKey(eventId), sessions);
        redis.opsForZSet().remove(heartbeatKey(eventId), sessions);
    }

    public void admitIfOpen(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        Instant now = Instant.now();


        boolean noLongerSelling = event == null
                || event.getStatus() != EventStatus.PUBLISHED
                || event.hasStarted(now);
        if (noLongerSelling) {
            redis.opsForSet().remove(ACTIVE_EVENTS, eventId.toString());
            return;
        }
        if (now.isBefore(event.getSaleOpensAt())) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        dropExpiredShoppers(eventId, nowMillis);
        long admitCount = Math.min(admitBatchSize, freeShopperSlots(eventId));
        if (admitCount <= 0) {
            return;
        }

        Set<String> nextInLine = redis.opsForZSet().range(queueKey(eventId), 0, admitCount - 1);
        if (nextInLine == null || nextInLine.isEmpty()) {
            return;
        }

        double admissionExpiresAtMillis = nowMillis + admissionTtl.toMillis();
        for (String sessionId : nextInLine) {
            redis.opsForZSet().add(activeKey(eventId), sessionId, admissionExpiresAtMillis);
            String token = UUID.randomUUID().toString();
            redis.opsForValue().set(tokenKey(token), eventId + ":" + sessionId, admissionTtl);
            redis.opsForValue().set(admittedKey(eventId, sessionId), token, admissionTtl);
        }

        Object[] admittedSessions = nextInLine.toArray();
        redis.opsForZSet().remove(queueKey(eventId), admittedSessions);
        redis.opsForZSet().remove(heartbeatKey(eventId), admittedSessions);
    }

    private void dropExpiredShoppers(UUID eventId, long nowMillis) {
        redis.opsForZSet().removeRangeByScore(activeKey(eventId), 0, nowMillis);
    }

    private long freeShopperSlots(UUID eventId) {
        Long activeShoppers = redis.opsForZSet().zCard(activeKey(eventId));
        return maxActiveShoppers - (activeShoppers == null ? 0 : activeShoppers);
    }

    private QueueStatusResponse admittedResponse(UUID eventId, String sessionId, String token) {
        Long secondsLeft = redis.getExpire(admittedKey(eventId, sessionId));
        Instant expiresAt = Instant.now().plusSeconds(secondsLeft != null && secondsLeft > 0 ? secondsLeft : 0);
        return QueueStatusResponse.admitted(token, expiresAt);
    }

    private static String queueKey(UUID eventId) { return "wr:" + eventId + ":queue"; }
    private static String heartbeatKey(UUID eventId) { return "wr:" + eventId + ":heartbeat"; }
    private static String activeKey(UUID eventId) { return "wr:" + eventId + ":active"; }
    private static String admittedKey(UUID eventId, String sessionId) { return "wr:" + eventId + ":admitted:" + sessionId; }
    private static String holdKey(UUID eventId, String admissionToken) { return "wr:" + eventId + ":hold:" + admissionToken; }
    private static String tokenKey(String token) { return "wr:token:" + token; }
}