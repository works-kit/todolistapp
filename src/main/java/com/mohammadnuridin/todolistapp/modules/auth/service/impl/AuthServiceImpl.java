// modules/auth/service/impl/AuthServiceImpl.java
package com.mohammadnuridin.todolistapp.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;
import com.mohammadnuridin.todolistapp.core.util.CookieUtil;
import com.mohammadnuridin.todolistapp.core.util.JwtService;
import com.mohammadnuridin.todolistapp.modules.auth.dto.AuthResponse;
import com.mohammadnuridin.todolistapp.modules.auth.dto.ChangePasswordRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.LoginRequest;
import com.mohammadnuridin.todolistapp.modules.auth.service.AuthService;
import com.mohammadnuridin.todolistapp.modules.auth.service.SessionService;
import com.mohammadnuridin.todolistapp.modules.auth.service.TokenBlacklistService;
import com.mohammadnuridin.todolistapp.modules.user.UserRepository;
import com.mohammadnuridin.todolistapp.modules.user.domain.User;
import com.mohammadnuridin.todolistapp.modules.user.domain.UserDetailsImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // ms

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration; // ms

    // ── LOGIN ────────────────────────────────────────────────
    @Override
    public AuthResponse login(LoginRequest req, boolean isWeb, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(req.email());

        if (!userDetails.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        long refreshTtlSec = refreshTokenExpiration / 1000;

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = sessionService.createRefreshToken(userDetails.getId(), refreshTtlSec);

        if (isWeb) {
            String sessionId = sessionService.createSession(userDetails.getId(), refreshTtlSec);
            sessionService.linkRefreshToSession(refreshToken, sessionId, refreshTtlSec); // ← tambah ini
            CookieUtil.setRefreshTokenCookie(response, refreshToken, refreshTtlSec);
            CookieUtil.setSessionIdCookie(response, sessionId, refreshTtlSec);
        }
        // Mobile: tidak ada cookie, tidak ada session — refresh_token dikembalikan di
        // body

        log.info("AUTH_LOGIN email={} userId={} client={}",
                userDetails.getEmail(), userDetails.getId(), isWeb ? "web" : "mobile");
        return new AuthResponse(accessToken, refreshToken, accessTokenExpiration / 1000);
    }

    // ── REFRESH ──────────────────────────────────────────────
    @Override
    public AuthResponse refresh(String rawRefreshToken, boolean isWeb,
            HttpServletResponse response) {
        // Lookup Redis → dapat userId, throw REFRESH_TOKEN_INVALID jika tidak ada
        String userId = sessionService.resolveRefreshToken(rawRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        long refreshTtlSec = refreshTokenExpiration / 1000;
        if (isWeb) {
            String oldSessionId = sessionService.getSessionIdByRefreshToken(rawRefreshToken);
            if (oldSessionId != null)
                sessionService.deleteSession(oldSessionId);
        }

        // Rotasi refresh token (revoke lama, buat baru)
        sessionService.revokeRefreshToken(rawRefreshToken);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = sessionService.createRefreshToken(userId, refreshTtlSec);

        if (isWeb) {
            // Rotasi session juga
            // Cari dan hapus session lama yang terkait user ini tidak mungkin
            // tanpa session ID — jadi kita buat session baru saja.
            // Session lama akan expired natural (TTL Redis).
            // Alternatif: kirim session_id juga dari controller jika diperlukan rotasi
            // ketat.
            String newSessionId = sessionService.createSession(userId, refreshTtlSec);
            CookieUtil.setRefreshTokenCookie(response, newRefreshToken, refreshTtlSec);
            CookieUtil.setSessionIdCookie(response, newSessionId, refreshTtlSec);
        }

        log.info("AUTH_REFRESH userId={} client={}", userId, isWeb ? "web" : "mobile");
        return new AuthResponse(newAccessToken, newRefreshToken, accessTokenExpiration / 1000);
    }

    // ── LOGOUT ───────────────────────────────────────────────
    @Override
    public void logout(HttpServletRequest request, String rawRefreshToken,
            boolean isWeb, HttpServletResponse response) {
        // 1. Blacklist current access token (JWT)
        String bearerToken = extractBearerToken(request);
        if (bearerToken != null) {
            long ttl = jwtService.getRemainingTtlSeconds(bearerToken);
            tokenBlacklistService.blacklist(bearerToken, ttl);
        }

        // 2. Revoke refresh token opaque dari Redis
        if (rawRefreshToken != null) {
            sessionService.revokeRefreshToken(rawRefreshToken);
        }

        if (isWeb) {
            // 3. Hapus session (hanya web yang punya session)
            try {
                String sessionId = CookieUtil.extractSessionIdFromCookie(request);
                sessionService.deleteSession(sessionId);
            } catch (AppException e) {
                log.debug("Session cookie not found during logout");
            }
            CookieUtil.clearRefreshTokenCookie(response);
            CookieUtil.clearSessionIdCookie(response);
        }

        log.info("AUTH_LOGOUT client={}", isWeb ? "web" : "mobile");
    }

    // ── CHANGE PASSWORD ──────────────────────────────────────
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest req,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String email = extractEmailFromToken(httpRequest);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        // Blacklist current access token
        String bearerToken = extractBearerToken(httpRequest);
        if (bearerToken != null) {
            long ttl = jwtService.getRemainingTtlSeconds(bearerToken);
            tokenBlacklistService.blacklist(bearerToken, ttl);
        }

        // Revoke semua token & session semua device (web + mobile)
        sessionService.revokeAllUserRefreshTokens(user.getId());
        sessionService.deleteAllUserSessions(user.getId());

        // Clear cookies untuk client ini (jika web)
        // Tidak bisa tahu isWeb di sini tanpa parameter tambahan,
        // tapi clear cookie tidak berbahaya untuk mobile (cookie tidak ada)
        CookieUtil.clearRefreshTokenCookie(httpResponse);
        CookieUtil.clearSessionIdCookie(httpResponse);

        log.warn("[AUDIT] CHANGE_PASSWORD userId={}", user.getId());
    }

    // ── Helpers ──────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null)
            throw new AppException(ErrorCode.UNAUTHORIZED);
        try {
            return jwtService.extractEmail(token);
        } catch (Exception e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }
}