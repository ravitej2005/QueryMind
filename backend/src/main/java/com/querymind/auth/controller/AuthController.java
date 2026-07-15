package com.querymind.auth.controller;

import com.querymind.auth.dto.AuthResponse;
import com.querymind.auth.dto.LoginRequest;
import com.querymind.auth.dto.RegisterRequest;
import com.querymind.auth.service.AuthService;
import com.querymind.common.exception.ApiException;
import com.querymind.common.ratelimit.RedisTokenBucketRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "qm_refresh";

    private final AuthService authService;
    private final RedisTokenBucketRateLimiter rateLimiter;

    public AuthController(AuthService authService, RedisTokenBucketRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "Register a new user and workspace")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req, HttpServletRequest http, HttpServletResponse res) {
        enforceRateLimit("register:" + clientIp(http));
        var pair = authService.register(req);
        setRefreshCookie(res, pair.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(pair.body());
    }

    @Operation(summary = "Log in with email/password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest http, HttpServletResponse res) {
        enforceRateLimit("login:" + clientIp(http));
        var pair = authService.login(req);
        setRefreshCookie(res, pair.refreshToken());
        return ResponseEntity.ok(pair.body());
    }

    @Operation(summary = "Exchange a valid refresh cookie for a new access token (rotates refresh token)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest http, HttpServletResponse res) {
        enforceRateLimit("refresh:" + clientIp(http));
        String refreshToken = readCookie(http, REFRESH_COOKIE);
        var pair = authService.refresh(refreshToken);
        setRefreshCookie(res, pair.refreshToken());
        return ResponseEntity.ok(pair.body());
    }

    @Operation(summary = "Log out the current session")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UUID userId, HttpServletRequest http, HttpServletResponse res) {
        String refreshToken = readCookie(http, REFRESH_COOKIE);
        authService.logout(userId, refreshToken);
        clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

    private void enforceRateLimit(String key) {
        if (!rateLimiter.tryConsume(key, 10, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many attempts, retry in " + rateLimiter.getRetryAfterSeconds(key) + "s");
        }
    }

    private void setRefreshCookie(HttpServletResponse res, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) Duration.ofDays(7).toSeconds());
        cookie.setAttribute("SameSite", "Strict");
        res.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse res) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
