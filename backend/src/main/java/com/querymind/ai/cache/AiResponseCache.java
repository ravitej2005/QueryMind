package com.querymind.ai.cache;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches AI responses per (connection, exact question text) to control cost
 * (prd.md Risks / FR11). Cache-aside: a miss always falls through to a real
 * AI call (memory.md §7).
 */
@Component
public class AiResponseCache {

    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redis;

    public AiResponseCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> get(UUID connectionId, String question) {
        return Optional.ofNullable(redis.opsForValue().get(key(connectionId, question)));
    }

    public void put(UUID connectionId, String question, String serializedResponse) {
        redis.opsForValue().set(key(connectionId, question), serializedResponse, TTL);
    }

    private String key(UUID connectionId, String question) {
        return "ai:response:" + connectionId + ":" + sha256(question.trim().toLowerCase());
    }

    private String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
