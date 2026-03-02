package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that logs every request and response passing through the gateway.
 * Runs at highest precedence (order = -1).
 *
 * Log format:
 *   → [GET] /api/v1/products  from 127.0.0.1
 *   ← [GET] /api/v1/products  200 OK  (23ms)
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String method = request.getMethod().name();
        String path   = request.getURI().getPath();
        String ip     = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        log.info("→ [{}] {}  from {}", method, path, ip);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("← [{}] {}  {}  ({}ms)", method, path, response.getStatusCode(), elapsed);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}
