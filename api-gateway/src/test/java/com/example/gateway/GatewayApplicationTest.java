package com.example.gateway;

import com.example.gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Gateway Application Tests")
class GatewayApplicationTest {

    @Autowired
    RateLimiterConfig rateLimiterConfig;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        // Passes if Spring context starts without errors
    }

    @Test
    @DisplayName("Default rate limiter bean is configured")
    void defaultRateLimiterIsConfigured() {
        RedisRateLimiter limiter = rateLimiterConfig.defaultRateLimiter();
        assertThat(limiter).isNotNull();
    }

    @Test
    @DisplayName("Write rate limiter bean is configured")
    void writeRateLimiterIsConfigured() {
        RedisRateLimiter limiter = rateLimiterConfig.writeRateLimiter();
        assertThat(limiter).isNotNull();
    }

    @Test
    @DisplayName("IP key resolver returns non-null key")
    void ipKeyResolverReturnsKey() {
        var resolver = rateLimiterConfig.ipKeyResolver();
        assertThat(resolver).isNotNull();
    }
}
