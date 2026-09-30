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
    import java.util.concurrent.ThreadLocalRandom;

    import java.time.Duration;
    import java.time.Instant;
    import java.util.Set;
    import java.util.UUID;
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

        // ---- Customer-facing ----

        public QueueJoinResponse join(UUID eventId) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
            if (event.getStatus() != EventStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not on sale");
            }

            String sessionId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            // Lottery for the pre-sale cohort: anyone joining before the sale opens gets a random
            // score in [0, 1), so their order is random and join speed gives no advantage.
            // Anyone joining after the sale opens gets their join timestamp (~1.7 trillion), which is
            // always larger, so they queue behind the whole pre-sale cohort, first come, first served.
            boolean beforeSale = Instant.now().isBefore(event.getSaleOpensAt());
            double queueScore = beforeSale ? ThreadLocalRandom.current().nextDouble() : now;

            redis.opsForSet().add(ACTIVE_EVENTS, eventId.toString());
            ZSetOperations<String, String> zset = redis.opsForZSet();
            zset.add(heartbeatKey(eventId), sessionId, now);
            zset.add(queueKey(eventId), sessionId, queueScore);

            return new QueueJoinResponse(sessionId, status(eventId, sessionId));
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

        // ---- Scheduler-facing ----

        public Set<UUID> activeEventIds() {
            Set<String> members = redis.opsForSet().members(ACTIVE_EVENTS);
            if (members == null) return Set.of();
            return members.stream().map(UUID::fromString).collect(Collectors.toSet());
        }

        public void sweepAbandoned(UUID eventId) {
            double cutoff = System.currentTimeMillis() - heartbeatTimeout.toMillis();
            Set<String> stale = redis.opsForZSet().rangeByScore(heartbeatKey(eventId), 0, cutoff);
            if (stale == null || stale.isEmpty()) return;

            Object[] sessions = stale.toArray();
            redis.opsForZSet().remove(queueKey(eventId), sessions);
            redis.opsForZSet().remove(heartbeatKey(eventId), sessions);
        }

        public void admitIfOpen(UUID eventId) {
            Event event = eventRepository.findById(eventId).orElse(null);

            // TODO: Event has no startsAt/end date, so a finished event that stays PUBLISHED is never
            //  deregistered here and remains in the public listing. Add Event.startsAt and deregister
            //  (and hide) events once it has passed.
            if (event == null || event.getStatus() != EventStatus.PUBLISHED) {
                redis.opsForSet().remove(ACTIVE_EVENTS, eventId.toString());
                return;
            }
            if (Instant.now().isBefore(event.getSaleOpensAt())) return;

            // Capacity check: only admit enough to fill free shopping slots.
            // wr:{eventId}:active holds current shoppers, scored by when their admission expires.
            long nowMs = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore(activeKey(eventId), 0, nowMs); // drop expired shoppers
            Long activeCount = redis.opsForZSet().zCard(activeKey(eventId));
            long freeSlots = maxActiveShoppers - (activeCount == null ? 0 : activeCount);
            long toAdmit = Math.min(admitBatchSize, freeSlots);
            if (toAdmit <= 0) return;

            Set<String> batch = redis.opsForZSet().range(queueKey(eventId), 0, toAdmit - 1);
            if (batch == null || batch.isEmpty()) return;

            double expiresAtMs = nowMs + admissionTtl.toMillis();

            for (String sessionId : batch) {
                redis.opsForZSet().add(activeKey(eventId), sessionId, expiresAtMs);
                String token = UUID.randomUUID().toString();
                redis.opsForValue().set(tokenKey(token), eventId + ":" + sessionId, admissionTtl);
                redis.opsForValue().set(admittedKey(eventId, sessionId), token, admissionTtl);
            }

            Object[] admitted = batch.toArray();
            redis.opsForZSet().remove(queueKey(eventId), admitted);
            redis.opsForZSet().remove(heartbeatKey(eventId), admitted);
        }

        // ---- Helpers ----

        private QueueStatusResponse admittedResponse(UUID eventId, String sessionId, String token) {
            Long ttlSeconds = redis.getExpire(admittedKey(eventId, sessionId));
            Instant expiresAt = Instant.now().plusSeconds(ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 0);
            return QueueStatusResponse.admitted(token, expiresAt);
        }

        private static String queueKey(UUID eventId) { return "wr:" + eventId + ":queue"; }
        private static String heartbeatKey(UUID eventId) { return "wr:" + eventId + ":heartbeat"; }
        private static String admittedKey(UUID eventId, String sessionId) { return "wr:" + eventId + ":admitted:" + sessionId; }
        private static String tokenKey(String token) { return "wr:token:" + token; }
            private static String activeKey(UUID eventId) { return "wr:" + eventId + ":active"; }
    }