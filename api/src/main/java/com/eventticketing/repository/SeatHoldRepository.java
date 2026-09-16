package com.eventticketing.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;


@Repository
public class SeatHoldRepository {

    private static final String KEY_PREFIX = "seat:hold:";

    private final StringRedisTemplate redisTemplate;

    public SeatHoldRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(UUID seatId, String holdToken, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key(seatId), holdToken, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public String getHoldToken(UUID seatId) {
        return redisTemplate.opsForValue().get(key(seatId));
    }

    public void release(UUID seatId) {
        redisTemplate.delete(key(seatId));
    }

    private String key(UUID seatId) {
        return KEY_PREFIX + seatId;
    }
}