package com.mohammadnuridin.todolistapp.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body untuk refresh token.
 *
 * Web → refresh_token tidak perlu di body (dibaca dari Cookie)
 * Mobile → refresh_token wajib di body
 *
 * Validasi dilakukan di controller berdasarkan ClientType.
 */
public record RefreshTokenRequest(

        @JsonProperty("refresh_token") String refreshToken // nullable untuk web client
) {
}