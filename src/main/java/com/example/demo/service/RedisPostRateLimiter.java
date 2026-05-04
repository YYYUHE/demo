package com.example.demo.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Redis 限流预留实现，后续接入 Redis 后可替换 DatabasePostRateLimiter。
 */
@Service("redisPostRateLimiter")
@Primary
public class RedisPostRateLimiter implements PostRateLimiter {

    private final DatabasePostRateLimiter fallback;

    public RedisPostRateLimiter(DatabasePostRateLimiter fallback) {
        this.fallback = fallback;
    }

    @Override
    public void validateCanPublish(Long userId) {
        fallback.validateCanPublish(userId);
    }
}
