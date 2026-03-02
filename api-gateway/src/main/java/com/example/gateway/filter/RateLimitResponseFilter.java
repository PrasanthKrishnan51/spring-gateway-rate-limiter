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
 *
 * Headers added on 429:
 *   X-RateLimit-Retry-After  : suggested wait time in seconds
 *   X-RateLimit-Message      : human-readable explanation
 */
@Slf4j
@Component
public class RateLimitResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();

            if (HttpStatus.TOO_MANY_REQUESTS.equals(response.getStatusCode())) {
                String ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
                String path = exchange.getRequest().getURI().getPath();

                log.warn("⚠ Rate limit exceeded — client: {}  path: {}", ip, path);

                // Add informative headers so clients know when to retry
                response.getHeaders().add("X-RateLimit-Retry-After", "1");
                response.getHeaders().add("X-RateLimit-Message",
                        "Too many requests. Token bucket exhausted. Retry after 1 second.");
            }
        }));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
