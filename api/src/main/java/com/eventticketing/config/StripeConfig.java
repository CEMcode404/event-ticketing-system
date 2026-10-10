package com.eventticketing.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!loadtest")
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(@Value("${stripe.secret-key}") String secretKey) {
        return new StripeClient(secretKey);
    }
}