package com.mohammadnuridin.todolistapp.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response token untuk login / refresh.
 *
 * Web → refresh_token NULL (dikirim via HttpOnly Cookie, bukan body)
 * Mobile → refresh_token ADA di body
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(

        @JsonProperty("access_token") String accessToken,

        @JsonProperty("refresh_token") String refreshToken, // null untuk web client

        @JsonProperty("token_type") String tokenType,

        @JsonProperty("expires_in") long expiresIn // access token TTL dalam detik
) {
    // Mobile — refresh token ada di body
    public static TokenResponse forMobile(String accessToken,
            String refreshToken,
            long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }

    // Web — refresh token tidak di body, ada di HttpOnly Cookie
    public static TokenResponse forWeb(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, null, "Bearer", expiresIn);
    }
}