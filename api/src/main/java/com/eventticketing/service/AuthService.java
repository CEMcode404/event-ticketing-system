package com.eventticketing.service;

import com.eventticketing.dto.LoginRequest;
import com.eventticketing.dto.LoginResponse;
import com.eventticketing.entity.User;
import com.eventticketing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final String SESSION_KEY_PREFIX = "admin:session:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final Duration sessionTtl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redisTemplate,
            @Value("${admin.session-ttl-hours:2}") long sessionTtlHours
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.sessionTtl = Duration.ofHours(sessionTtlHours);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + token, user.getUsername(), sessionTtl);

        return new LoginResponse(token);
    }

    public Optional<String> validate(String token) {
        String username = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
        return Optional.ofNullable(username);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}