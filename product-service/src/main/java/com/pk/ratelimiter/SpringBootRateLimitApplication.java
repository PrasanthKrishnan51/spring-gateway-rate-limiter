package com.pk.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpringBootRateLimitApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootRateLimitApplication.class, args);
    }
}
