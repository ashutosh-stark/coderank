package com.ashutosh.coderank.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String username) {
        return cache.computeIfAbsent(username, k -> 
            Bucket4j.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build()
        );
    }

    public boolean allowRequest(String username) {
        Bucket bucket = resolveBucket(username);
        return bucket.tryConsume(1);
    }
}
