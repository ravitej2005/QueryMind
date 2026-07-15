package com.querymind.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates access/refresh JWTs. Access token lives in frontend
 * memory only (never localStorage). Refresh token is delivered as an
 * HttpOnly cookie by AuthController. See memory.md §2 (do not silently
 * reverse this decision).
 */
@Service
public class JwtService {

    private final Key accessKey;
    private final Key refreshKey;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-ttl-minutes}") long accessTtlMinutes,
            @Value("${jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.accessKey = Keys.hmacShaKeyFor(pad(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(pad(refreshSecret));
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    // HMAC-SHA256 requires >= 256-bit key; fail loudly instead of silently
    // truncating a misconfigured short secret in prod.
    private byte[] pad(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set JWT_ACCESS_SECRET / JWT_REFRESH_SECRET.");
        }
        byte[] bytes = secret.getBytes();
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes.");
        }
        return bytes;
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtlDays, ChronoUnit.DAYS)))
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) accessKey).build()
                .parseSignedClaims(token).getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) refreshKey).build()
                .parseSignedClaims(token).getPayload();
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }
}
