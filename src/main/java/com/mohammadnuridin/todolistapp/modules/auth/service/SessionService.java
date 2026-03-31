package com.mohammadnuridin.todolistapp.modules.auth.service;

public interface SessionService {
    String createSession(String userId, long ttlSeconds);

    boolean isSessionValid(String sessionId);

    void deleteSession(String sessionId);

    void deleteAllUserSessions(String userId);

    String createRefreshToken(String userId, long ttlSeconds);

    String resolveRefreshToken(String refreshToken); // return userId, throw jika invalid

    void revokeRefreshToken(String refreshToken);

    void revokeAllUserRefreshTokens(String userId);

    // Tambah ke SessionService interface:
    void linkRefreshToSession(String refreshToken, String sessionId, long ttlSeconds);

    String getSessionIdByRefreshToken(String refreshToken);
}