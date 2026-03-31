package com.mohammadnuridin.todolistapp.modules.auth.dto;

/**
 * Internal result dari AuthService — membawa kedua token.
 * Controller yang memutuskan mana yang masuk body vs Cookie
 * berdasarkan ClientType.
 *
 * Tidak di-expose langsung ke client — hanya dipakai internal.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken, // selalu ada — controller yang pilih cara kirimnya
        long expiresIn // access token TTL dalam detik
) {
    /**
     * Konversi ke TokenResponse sesuai client type.
     *
     * Web → refresh_token null di body (ada di Cookie)
     * Mobile → refresh_token ada di body
     */
    public TokenResponse toResponse(boolean isWeb) {
        return isWeb
                ? TokenResponse.forWeb(accessToken, expiresIn)
                : TokenResponse.forMobile(accessToken, refreshToken, expiresIn);
    }
}