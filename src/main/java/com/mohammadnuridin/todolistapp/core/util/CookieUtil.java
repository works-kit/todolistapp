package com.mohammadnuridin.todolistapp.core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;

public final class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String SESSION_ID_COOKIE = "session_id";
    private static final String COOKIE_PATH = "/api/auth";

    private CookieUtil() {
    }

    /**
     * Set HttpOnly Cookie berisi refresh token.
     *
     * @param response      HttpServletResponse
     * @param token         refresh token string
     * @param maxAgeSeconds TTL cookie dalam detik
     */
    public static void setRefreshTokenCookie(HttpServletResponse response,
            String token,
            long maxAgeSeconds) {
        response.addHeader("Set-Cookie",
                REFRESH_TOKEN_COOKIE + "=" + token
                        + "; Max-Age=" + maxAgeSeconds
                        + "; Path=" + COOKIE_PATH
                        + "; HttpOnly"
                        + "; SameSite=Lax"
        // + "; Secure" // wajib aktifkan di production (HTTPS)
        );
    }

    /**
     * Hapus cookie refresh token saat logout.
     * Max-Age=0 instruksikan browser untuk langsung hapus cookie.
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                REFRESH_TOKEN_COOKIE + "="
                        + "; Max-Age=0"
                        + "; Path=" + COOKIE_PATH
                        + "; HttpOnly"
                        + "; SameSite=Lax");
    }

    /**
     * Baca refresh token dari cookie request.
     * Throw REFRESH_TOKEN_INVALID jika cookie tidak ada.
     */
    public static String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return Arrays.stream(cookies)
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    public static void clearSessionIdCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                SESSION_ID_COOKIE + "="
                        + "; Max-Age=0"
                        + "; Path=" + COOKIE_PATH
                        + "; HttpOnly"
                        + "; SameSite=Lax");
    }

    public static void setSessionIdCookie(HttpServletResponse response, String sessionId, long refreshTtlSec) {
        response.addHeader("Set-Cookie",
                SESSION_ID_COOKIE + "=" + sessionId
                        + "; Max-Age=" + refreshTtlSec
                        + "; Path=" + COOKIE_PATH
                        + "; HttpOnly"
                        + "; SameSite=Lax");
    }

    public static String extractSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return Arrays.stream(cookies)
                .filter(c -> SESSION_ID_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
    }
}