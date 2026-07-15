package com.querymind.auth.security;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed refresh-token registry: tracks the current valid jti per user
 * (rotation) and a blocklist of jtis that have already been consumed/revoked
 * (reuse detection). Redis is cache/session-store only here, never the
 * system of record for user data (memory.md §2).
 */
@Component
public class RefreshTokenStore {

    private static final String ACTIVE_PREFIX = "refresh:active:";
    private static final String BLOCKED_PREFIX = "refresh:blocked:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void storeActive(UUID userId, String jti, Duration ttl) {
        redis.opsForValue().set(ACTIVE_PREFIX + userId, jti, ttl);
    }

    public boolean isActive(UUID userId, String jti) {
        String current = redis.opsForValue().get(ACTIVE_PREFIX + userId);
        return jti.equals(current);
    }

    public boolean isBlocked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(BLOCKED_PREFIX + jti));
    }

    public void block(String jti, Duration ttl) {
        redis.opsForValue().set(BLOCKED_PREFIX + jti, "1", ttl);
    }

    /** Reuse-detection: revoke the whole session (logout-everywhere) for this user. */
    public void revokeAll(UUID userId) {
        redis.delete(ACTIVE_PREFIX + userId);
    }
}
