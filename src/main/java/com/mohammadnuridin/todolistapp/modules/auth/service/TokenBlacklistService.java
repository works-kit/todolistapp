package com.mohammadnuridin.todolistapp.modules.auth.service;

public interface TokenBlacklistService {
    void blacklist(String token, long ttlSeconds);

    boolean isBlacklisted(String token);
}