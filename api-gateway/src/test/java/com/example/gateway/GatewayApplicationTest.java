package com.example.gateway;

import com.example.gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Gateway Application Tests")
class GatewayApplicationTest {

    @Autowired
    RateLimiterConfig rateLimiterConfig;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("IP key resolver returns non-null key")
    void ipKeyResolverReturnsKey() {
        var resolver = rateLimiterConfig.ipKeyResolver();
        assertThat(resolver).isNotNull();
    }
}
