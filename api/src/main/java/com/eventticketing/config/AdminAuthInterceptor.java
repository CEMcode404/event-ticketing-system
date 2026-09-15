package com.eventticketing.config;

import com.eventticketing.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AdminAuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws java.io.IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or malformed Authorization header");
            return false;
        }

        String token = header.substring(BEARER_PREFIX.length());

        if (authService.validate(token).isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired session");
            return false;
        }

        return true;
    }
}