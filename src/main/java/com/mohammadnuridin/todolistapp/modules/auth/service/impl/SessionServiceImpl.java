package com.mohammadnuridin.todolistapp.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;
import com.mohammadnuridin.todolistapp.modules.auth.service.SessionService;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_REFRESH_PREFIX = "user_refresh:";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    // Implementasi di SessionServiceImpl:
    private static final String REFRESH_SESSION_PREFIX = "refresh_session:";

    // ── SESSION ──────────────────────────────────────────────

    @Override
    public void linkRefreshToSession(String refreshToken, String sessionId, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(REFRESH_SESSION_PREFIX + refreshToken, sessionId, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public String getSessionIdByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(REFRESH_SESSION_PREFIX + refreshToken);
    }

    @Override
    public String createSession(String userId, long ttlSeconds) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue()
                .set(SESSION_PREFIX + sessionId, userId, Duration.ofSeconds(ttlSeconds));

        // track per user untuk mass-invalidation
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, Duration.ofSeconds(ttlSeconds));

        return sessionId;
    }

    @Override
    public boolean isSessionValid(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_PREFIX + sessionId));
    }

    @Override
    public void deleteSession(String sessionId) {
        String userId = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        redisTemplate.delete(SESSION_PREFIX + sessionId);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + userId, sessionId);
        }
    }

    @Override
    public void deleteAllUserSessions(String userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds != null) {
            sessionIds.forEach(sid -> redisTemplate.delete(SESSION_PREFIX + sid));
        }
        redisTemplate.delete(userSessionsKey);
        log.info("Deleted all sessions for userId={}", userId);
    }

    // ── REFRESH TOKEN (opaque) ────────────────────────────────

    @Override
    public String createRefreshToken(String userId, long ttlSeconds) {
        // 32 bytes → 43 karakter URL-safe Base64 (tanpa padding)
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // refresh:{token} → userId
        redisTemplate.opsForValue()
                .set(REFRESH_PREFIX + token, userId, Duration.ofSeconds(ttlSeconds));

        // track per user untuk mass-revocation
        String userRefreshKey = USER_REFRESH_PREFIX + userId;
        redisTemplate.opsForSet().add(userRefreshKey, token);
        redisTemplate.expire(userRefreshKey, Duration.ofSeconds(ttlSeconds));

        return token;
    }

    @Override
    public String resolveRefreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        if (userId == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return userId;
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_REFRESH_PREFIX + userId, refreshToken);
        }
    }

    @Override
    public void revokeAllUserRefreshTokens(String userId) {
        String userRefreshKey = USER_REFRESH_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userRefreshKey);
        if (tokens != null) {
            tokens.forEach(t -> redisTemplate.delete(REFRESH_PREFIX + t));
        }
        redisTemplate.delete(userRefreshKey);
        log.info("Revoked all refresh tokens for userId={}", userId);
    }
}