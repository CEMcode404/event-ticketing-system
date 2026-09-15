package com.eventticketing.config;

import com.eventticketing.entity.User;
import com.eventticketing.enums.UserType;
import com.eventticketing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at every startup. Idempotent — if the admin username already
 * has a row, does nothing. This is how the single admin User gets into
 * the database at all: no manual INSERT, no registration endpoint. The
 * plaintext password only ever lives in env config (local: a fallback in
 * application-local.properties; prod: required, no fallback, pulled from
 * Secrets Manager) — what actually lands in the users table is the
 * BCrypt hash, never the plaintext.
 */
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }

        User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), UserType.ADMIN);
        userRepository.save(admin);
    }
}