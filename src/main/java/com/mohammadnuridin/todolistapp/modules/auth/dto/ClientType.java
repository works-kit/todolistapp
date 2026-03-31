package com.mohammadnuridin.todolistapp.modules.auth.dto;

/**
 * Tipe client yang mengakses API.
 * Ditentukan dari header X-Client-Type pada setiap request.
 *
 * Web → refresh token via HttpOnly Cookie
 * Mobile → refresh token via response body
 */
public enum ClientType {
    WEB,
    MOBILE;

    /**
     * Parse dari header X-Client-Type.
     * Default ke MOBILE jika header tidak ada atau tidak dikenali,
     * supaya behavior aman untuk client yang tidak kirim header.
     */
    public static ClientType from(String headerValue) {
        if ("web".equalsIgnoreCase(headerValue))
            return WEB;
        return MOBILE;
    }
}