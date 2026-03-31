package com.mohammadnuridin.todolistapp.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mohammadnuridin.todolistapp.modules.auth.service.TokenBlacklistService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String token, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue()
                    .set(PREFIX + token, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}