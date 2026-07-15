package com.querymind.auth.service;

import com.querymind.auth.domain.User;
import com.querymind.auth.dto.AuthResponse;
import com.querymind.auth.dto.LoginRequest;
import com.querymind.auth.dto.RegisterRequest;
import com.querymind.auth.repository.UserRepository;
import com.querymind.auth.security.JwtService;
import com.querymind.auth.security.RefreshTokenStore;
import com.querymind.common.exception.ApiException;
import com.querymind.workspace.service.WorkspaceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final WorkspaceService workspaceService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            WorkspaceService workspaceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.workspaceService = workspaceService;
    }

    public record TokenPair(String accessToken, String refreshToken, AuthResponse body) {}

    @Transactional
    public TokenPair register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = userRepository.save(
                new User(req.email(), passwordEncoder.encode(req.password()), req.displayName()));
        workspaceService.createWorkspaceForNewUser(user.getId(), user.getDisplayName());
        return issueTokens(user);
    }

    @Transactional
    public TokenPair login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MISSING_REFRESH_TOKEN", "No refresh token provided");
        }
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token invalid or expired");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        String jti = claims.getId();

        if (refreshTokenStore.isBlocked(jti)) {
            // Reuse of an already-rotated-out token: possible theft, revoke the whole session.
            refreshTokenStore.revokeAll(userId);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE", "Session revoked, please log in again");
        }
        if (!refreshTokenStore.isActive(userId, jti)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token invalid or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "User not found"));

        // Rotate: block the old jti, issue a new one.
        refreshTokenStore.block(jti, Duration.ofDays(jwtService.getRefreshTtlDays()));
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId, String refreshToken) {
        refreshTokenStore.revokeAll(userId);
        if (refreshToken != null) {
            try {
                Claims claims = jwtService.parseRefreshToken(refreshToken);
                refreshTokenStore.block(claims.getId(), Duration.ofDays(jwtService.getRefreshTtlDays()));
            } catch (JwtException | IllegalArgumentException ignored) {
                // already invalid, nothing to block
            }
        }
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getId(), jti);
        refreshTokenStore.storeActive(user.getId(), jti, Duration.ofDays(jwtService.getRefreshTtlDays()));
        AuthResponse body = new AuthResponse(accessToken, user.getId(), user.getEmail(), user.getDisplayName());
        return new TokenPair(accessToken, refreshToken, body);
    }
}
