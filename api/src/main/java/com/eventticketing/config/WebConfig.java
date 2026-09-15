package com.eventticketing.config;

import com.eventticketing.service.AuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public WebConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(authService))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }
}