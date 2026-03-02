package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enriches 429 Too Many Requests responses with standard rate limit headers
 * and logs the throttled client details.
 * <p>
 * Headers added on 429:
 * X-RateLimit-Retry-After  : suggested wait time in seconds
 * X-RateLimit-Message      : human-readable explanation
 */
@Slf4j
@Component
public class RateLimitResponseFilter implements GlobalFilter, Ordered {

    private static final String RETRY_AFTER_SECONDS = "1";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();

                    if (HttpStatus.TOO_MANY_REQUESTS.equals(response.getStatusCode())) {

                        String clientIp = extractClientIp(exchange);
                        String path = exchange.getRequest().getURI().getPath();

                        log.warn("Rate limit exceeded | client={} | path={}", clientIp, path);

                        response.getHeaders().set("X-RateLimit-Retry-After", RETRY_AFTER_SECONDS);
                        response.getHeaders().set("X-RateLimit-Message",
                                "Too many requests. Please retry after " + RETRY_AFTER_SECONDS + " second(s).");
                    }
                }));
    }

    private String extractClientIp(ServerWebExchange exchange) {

        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0];
        }

        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
