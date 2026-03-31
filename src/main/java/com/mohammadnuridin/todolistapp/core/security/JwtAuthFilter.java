package com.mohammadnuridin.todolistapp.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mohammadnuridin.todolistapp.core.config.SecurityConfig;
import com.mohammadnuridin.todolistapp.core.util.JwtService;
import com.mohammadnuridin.todolistapp.modules.auth.service.TokenBlacklistService;
import com.mohammadnuridin.todolistapp.modules.auth.service.impl.UserDetailsServiceImpl;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ── Tidak ada Bearer token — lanjutkan tanpa auth ────
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // ── Cek blacklist (logout) ────────────────────────
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Blacklisted token used: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
                return;
            }

            // ── Extract email dari token ──────────────────────
            final String email = jwtService.extractEmail(token);

            // ── Set auth hanya jika belum ada di SecurityContext
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            log.warn("JWT filter error at {}: {}", request.getRequestURI(), e.getMessage());
            // Tidak throw — biarkan SecurityContext kosong, Spring Security handle
            // selanjutnya
        }

        filterChain.doFilter(request, response);
    }

    // ── Skip filter untuk endpoint publik ────────────────────
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter untuk semua PUBLIC_ENDPOINTS
        for (String pattern : SecurityConfig.PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, path))
                return true;
        }
        return false;
    }

}