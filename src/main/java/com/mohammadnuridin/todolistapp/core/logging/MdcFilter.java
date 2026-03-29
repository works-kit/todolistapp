package com.mohammadnuridin.todolistapp.core.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populate MDC (Mapped Diagnostic Context) untuk setiap request.
 *
 * Field yang diisi:
 * requestId → trace ID unik per request (X-Request-ID header atau UUID baru)
 * method → HTTP method
 * path → request URI
 * userId → ID user yang sedang login (jika ada)
 * clientIp → IP client (support X-Forwarded-For)
 *
 * Semua field MDC ini akan otomatis muncul di setiap log line
 * dalam satu request yang sama — sangat berguna untuk tracing di Grafana Loki.
 */
@Component
@Order(1) // Jalankan paling awal
public class MdcFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Request ID — dari header jika ada, atau generate baru
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            MDC.put("requestId", requestId);
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());
            MDC.put("clientIp", getClientIp(request));

            // Set request ID ke response header untuk client tracking
            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);

            // Tambah userId setelah filter chain (SecurityContext sudah terisi)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put("userId", auth.getName());
            }

        } finally {
            // Wajib clear MDC setelah request selesai — cegah memory leak di thread pool
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim(); // Ambil IP pertama dari chain proxy
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank())
            return ip;
        return request.getRemoteAddr();
    }
}
