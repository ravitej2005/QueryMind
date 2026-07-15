package com.querymind.common.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Simple fixed-window token-bucket rate limiter backed by Redis, shared by
 * auth endpoints (brute-force protection) and AI endpoints (cost control),
 * per rules.md §10 / memory.md §7.
 */
@Component
public class RedisTokenBucketRateLimiter {

    private final StringRedisTemplate redis;

    public RedisTokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @return true if the call is allowed, false if the caller is over the limit.
     */
    public boolean tryConsume(String key, int maxRequests, Duration window) {
        Long count = redis.opsForValue().increment("ratelimit:" + key);
        if (count != null && count == 1L) {
            redis.expire("ratelimit:" + key, window);
        }
        return count != null && count <= maxRequests;
    }

    public long getRetryAfterSeconds(String key) {
        Long ttl = redis.getExpire("ratelimit:" + key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }
}
