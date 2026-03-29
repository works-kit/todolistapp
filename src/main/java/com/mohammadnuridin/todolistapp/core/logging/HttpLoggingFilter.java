package com.mohammadnuridin.todolistapp.core.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Log setiap HTTP request & response:
 * → method, path, status, durasi (ms)
 * → warning jika durasi > SLOW_REQUEST_THRESHOLD_MS
 *
 * Path yang di-skip:
 * - /internal/actuator/** (health check spam)
 * - /favicon.ico
 */
@Slf4j
@Component
@Order(2)
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Value("${app.logging.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    private static final Set<String> SKIP_PATHS = Set.of(
            "/internal/actuator",
            "/actuator",
            "/favicon.ico");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String path = request.getRequestURI();
            String method = request.getMethod();

            // Log level berdasarkan status code & durasi
            if (duration >= slowRequestThresholdMs) {
                log.warn("SLOW [{} {}] status={} duration={}ms (threshold={}ms)",
                        method, path, status, duration, slowRequestThresholdMs);
            } else if (status >= 500) {
                log.error("HTTP [{} {}] status={} duration={}ms",
                        method, path, status, duration);
            } else if (status >= 400) {
                log.warn("HTTP [{} {}] status={} duration={}ms",
                        method, path, status, duration);
            } else {
                log.info("HTTP [{} {}] status={} duration={}ms",
                        method, path, status, duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }
}
