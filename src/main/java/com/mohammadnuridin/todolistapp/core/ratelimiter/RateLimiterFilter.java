package com.mohammadnuridin.todolistapp.core.ratelimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Rate limiter filter berbasis Redis (distributed).
 *
 * Perbedaan dari versi Caffeine (in-memory):
 * - State disimpan di Redis → konsisten di semua instance (horizontal scaling).
 * - Atomic via Lua script → tidak ada race condition.
 * - Fail-open: jika Redis down, request tetap diizinkan (availability >
 * security).
 *
 * Dua bucket terpisah:
 * - "global" → semua endpoint (capacity & window dari props default)
 * - "auth" → endpoint /auth/** (lebih ketat)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RedisRateLimiter redisRateLimiter;
    private final RateLimiterProperties props;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.contains("/auth/");

        // Pilih bucket & config sesuai endpoint
        String prefix;
        int capacity;
        long windowSeconds;

        if (isAuthEndpoint) {
            prefix = "auth";
            capacity = props.getAuth().getCapacity();
            windowSeconds = props.getAuth().getRefillDurationSeconds();
        } else {
            prefix = "global";
            capacity = props.getCapacity();
            windowSeconds = props.getRefillDurationSeconds();
        }

        RedisRateLimiter.RateLimitResult result = redisRateLimiter.tryConsume(
                prefix, clientIp, capacity, windowSeconds);

        // Set informatif rate-limit headers (RFC 6585 style)
        response.setIntHeader("X-RateLimit-Limit", capacity);
        response.setIntHeader("X-RateLimit-Remaining", result.remaining());
        response.setDateHeader("X-RateLimit-Reset", result.resetEpochMs());

        if (!result.allowed()) {
            log.warn("[RateLimit] DENIED — IP: {}, path: {}, bucket: {}", clientIp, path, prefix);
            sendTooManyRequestsResponse(response, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolusi IP client dengan mempertimbangkan reverse proxy.
     * Prioritas: X-Forwarded-For → X-Real-IP → RemoteAddr
     *
     * Catatan: di production, pastikan hanya trusted proxy yang bisa
     * set X-Forwarded-For agar tidak bisa di-spoof client.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Ambil IP pertama (original client), sisanya proxy chain
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response,
            long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        Map<String, Object> body = Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please try again later.",
                "timestamp", Instant.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}