// modules/auth/api/AuthController.java
package com.mohammadnuridin.todolistapp.modules.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;
import com.mohammadnuridin.todolistapp.core.response.ApiResponse;
import com.mohammadnuridin.todolistapp.core.util.CookieUtil;
import com.mohammadnuridin.todolistapp.core.util.MessageService;
import com.mohammadnuridin.todolistapp.modules.auth.dto.AuthResponse;
import com.mohammadnuridin.todolistapp.modules.auth.dto.ChangePasswordRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.ClientType;
import com.mohammadnuridin.todolistapp.modules.auth.dto.LoginRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.LogoutRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.RefreshTokenRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.TokenResponse;
import com.mohammadnuridin.todolistapp.modules.auth.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MessageService msg;

    // ── POST /api/auth/login ─────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "mobile") String clientTypeHeader,
            HttpServletResponse response) {

        boolean isWeb = ClientType.from(clientTypeHeader) == ClientType.WEB;

        // Service menangani cookie & session berdasarkan isWeb
        AuthResponse authResponse = authService.login(request, isWeb, response);

        return ResponseEntity.ok(
                ApiResponse.ok(msg.get("success.login"), authResponse.toResponse(isWeb)));
    }

    // ── POST /api/auth/refresh-token ─────────────────────────
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            @RequestHeader(value = "X-Client-Type", defaultValue = "mobile") String clientTypeHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean isWeb = ClientType.from(clientTypeHeader) == ClientType.WEB;

        // Resolve refresh token: web dari Cookie, mobile dari body
        String rawRefreshToken = isWeb
                ? CookieUtil.extractRefreshTokenFromCookie(request)
                : extractRefreshFromBody(body);

        AuthResponse authResponse = authService.refresh(rawRefreshToken, isWeb, response);

        return ResponseEntity.ok(
                ApiResponse.ok(msg.get("success.token.refreshed"), authResponse.toResponse(isWeb)));
    }

    // ── POST /api/auth/logout ────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) LogoutRequest body,
            @RequestHeader(value = "X-Client-Type", defaultValue = "mobile") String clientTypeHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean isWeb = ClientType.from(clientTypeHeader) == ClientType.WEB;

        // Resolve refresh token untuk di-revoke
        String rawRefreshToken = isWeb
                ? safeExtractRefreshFromCookie(request) // web: dari Cookie
                : extractRefreshFromBody(body != null // mobile: dari body
                        ? new RefreshTokenRequest(body.refreshToken())
                        : null);

        authService.logout(request, rawRefreshToken, isWeb, response);

        return ResponseEntity.ok(ApiResponse.ok(msg.get("success.logout")));
    }

    // ── PUT /api/auth/password ───────────────────────────────
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        authService.changePassword(request, httpRequest, httpResponse);
        return ResponseEntity.ok(
                ApiResponse.ok(msg.get("success.user.password_changed")));
    }

    // ── Helpers ──────────────────────────────────────────────

    private String extractRefreshFromBody(RefreshTokenRequest body) {
        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return body.refreshToken();
    }

    /** Web logout: jika cookie tidak ada, tetap lanjut (mungkin sudah expired) */
    private String safeExtractRefreshFromCookie(HttpServletRequest request) {
        try {
            return CookieUtil.extractRefreshTokenFromCookie(request);
        } catch (AppException e) {
            return null;
        }
    }
}