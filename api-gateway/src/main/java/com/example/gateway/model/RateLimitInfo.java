package com.example.gateway.model;

import lombok.Builder;

/**
 * Snapshot of the current token bucket state for a given client key.
 * Returned by the /gateway/rate-limit/status endpoint.
 */
@Builder
public record RateLimitInfo(
        String key,
        long remainingTokens,
        int replenishRate,
        int burstCapacity,
        String type       // "read" or "write"
) {}
