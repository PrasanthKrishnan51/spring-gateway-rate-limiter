package com.example.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Management endpoint to inspect the live state of token buckets in Redis.
 *
 * GET  /gateway/rate-limit/status?key={clientKey}
 *      → Returns tokens remaining, replenish rate, burst capacity
 *
 * DELETE /gateway/rate-limit/reset?key={clientKey}
 *      → Resets (deletes) the token bucket for a key — useful for testing
 */
@Slf4j
@RestController
@RequestMapping("/gateway/rate-limit")
@RequiredArgsConstructor
public class RateLimitStatusController {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    /**
     * Returns the remaining token count for a given key.
     *
     * Redis keys used by Spring Cloud Gateway's RedisRateLimiter:
     *   request_rate_limiter.{key}.tokens
     *   request_rate_limiter.{key}.timestamp
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> getStatus(
            @RequestParam(defaultValue = "127.0.0.1") String key) {

        String tokensKey    = "request_rate_limiter." + key + ".tokens";
        String timestampKey = "request_rate_limiter." + key + ".timestamp";

        return reactiveRedisTemplate.opsForValue().get(tokensKey)
                .zipWith(reactiveRedisTemplate.opsForValue().get(timestampKey))
                .map(tuple -> {
                    Map<String, Object> body = Map.of(
                            "key",            key,
                            "remainingTokens", tuple.getT1(),
                            "lastRefill",      tuple.getT2(),
                            "readLimit",       Map.of("replenishRate", 10, "burstCapacity", 20),
                            "writeLimit",      Map.of("replenishRate", 5,  "burstCapacity", 10)
                    );
                    return ResponseEntity.ok(body);
                })
                .defaultIfEmpty(ResponseEntity.ok(Map.of(
                        "key",    key,
                        "status", "No bucket found — client has not made any requests yet or bucket is full"
                )));
    }

    /**
     * Resets the token bucket for a specific key.
     * Handy for testing rate limit recovery without waiting for TTL.
     */
    @DeleteMapping("/reset")
    public Mono<ResponseEntity<Map<String, String>>> resetBucket(
            @RequestParam String key) {

        String tokensKey    = "request_rate_limiter." + key + ".tokens";
        String timestampKey = "request_rate_limiter." + key + ".timestamp";

        log.warn("Resetting rate limit bucket for key: {}", key);

        return reactiveRedisTemplate.delete(tokensKey, timestampKey)
                .map(deleted -> ResponseEntity.ok(Map.of(
                        "message", "Rate limit bucket reset for key: " + key,
                        "deleted", deleted + " Redis keys removed"
                )));
    }
}
