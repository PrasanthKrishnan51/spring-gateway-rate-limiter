package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Token Bucket Rate Limiting via Redis.
 * Spring Cloud Gateway's RedisRateLimiter implements the Token Bucket algorithm:
 *
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Referenced in application.yml as: key-resolver: "#{@apiKeyResolver}"
     * Used by product-read route (GET requests)
     * Identifies client by X-API-Key header, falls back to IP
     */
    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                return Mono.just("api-key:" + apiKey);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Referenced in application.yml as: key-resolver: "#{@ipKeyResolver}"
     * Used by product-write route (POST/PUT/DELETE requests)
     * Identifies client by IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
