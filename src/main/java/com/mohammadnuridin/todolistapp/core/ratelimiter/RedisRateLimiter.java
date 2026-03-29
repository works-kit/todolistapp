package com.mohammadnuridin.todolistapp.core.ratelimiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Redis-based Token Bucket menggunakan Lua script.
 *
 * Kenapa Lua script?
 * - Atomic: GET + SET dieksekusi dalam satu operasi di Redis server.
 * Tidak ada race condition antar instance aplikasi (horizontally scaled).
 * - Efficient: satu round-trip ke Redis per request.
 *
 * Algoritma: Fixed Window Counter
 * - Key Redis: "rate:{prefix}:{clientIp}" → nilai = sisa token
 * - Jika key belum ada (pertama kali / sudah expired): set ke capacity-1, TTL =
 * windowSeconds
 * - Jika key ada dan > 0: DECR
 * - Jika key ada dan == 0: tolak
 */
@Slf4j
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(
            @Qualifier("rateLimiterRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    } // bean: rateLimiterRedisTemplate

    /**
     * Lua script — dieksekusi atomik di Redis.
     *
     * KEYS[1] = Redis key (e.g. "rate:global:192.168.1.1")
     * ARGV[1] = capacity (jumlah max token)
     * ARGV[2] = window TTL dalam detik
     *
     * Return: {remaining_tokens, ttl_millis}
     * remaining_tokens >= 0 → request diizinkan (nilai setelah dikurangi)
     * remaining_tokens == -1 → rate limit tercapai, tolak request
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = """
            local key       = KEYS[1]
            local capacity  = tonumber(ARGV[1])
            local window    = tonumber(ARGV[2])

            local current = redis.call('GET', key)

            if current == false then
                -- Key belum ada: inisialisasi bucket, set TTL
                redis.call('SET', key, capacity - 1, 'EX', window)
                local ttl = redis.call('PTTL', key)
                return {capacity - 1, ttl}
            end

            local tokens = tonumber(current)

            if tokens <= 0 then
                -- Bucket kosong: tolak
                local ttl = redis.call('PTTL', key)
                return {-1, ttl}
            end

            -- Kurangi token, kembalikan sisa
            local remaining = redis.call('DECR', key)
            local ttl = redis.call('PTTL', key)
            return {remaining, ttl}
            """;

    private static final DefaultRedisScript<List> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(RATE_LIMIT_LUA_SCRIPT);
        SCRIPT.setResultType(List.class);
    }

    /**
     * Coba konsumsi 1 token dari bucket Redis untuk clientIp.
     *
     * @param prefix        "global" atau "auth" — namespace key Redis
     * @param clientIp      IP address client
     * @param capacity      Kapasitas maksimum bucket
     * @param windowSeconds Durasi window dalam detik
     * @return RateLimitResult berisi status allow/deny + metadata untuk headers
     */
    public RateLimitResult tryConsume(String prefix, String clientIp, int capacity, long windowSeconds) {
        String key = "rate:" + prefix + ":" + clientIp;

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = (List<Long>) redisTemplate.execute(
                    SCRIPT,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(windowSeconds));

            if (result == null || result.size() < 2) {
                // Redis error / null → fail open (izinkan request)
                log.warn("Redis rate limiter returned null for key: {} — failing open", key);
                return RateLimitResult.allowed(capacity - 1, windowSeconds, Instant.now().toEpochMilli());
            }

            long remaining = result.get(0);
            long ttlMillis = result.get(1);

            // Hitung epoch ms kapan window reset
            long resetEpochMs = ttlMillis > 0
                    ? Instant.now().toEpochMilli() + ttlMillis
                    : Instant.now().toEpochMilli() + (windowSeconds * 1000L);

            if (remaining < 0) {
                return RateLimitResult.denied(capacity, resetEpochMs);
            }

            return RateLimitResult.allowed((int) remaining, windowSeconds, resetEpochMs);

        } catch (Exception e) {
            // Redis down → fail open agar app tetap bisa melayani request
            log.error("Redis rate limiter error for key: {} — failing open. Cause: {}", key, e.getMessage());
            return RateLimitResult.allowed(capacity - 1, windowSeconds, Instant.now().toEpochMilli());
        }
    }

    /**
     * Result dari satu operasi rate limit.
     */
    public record RateLimitResult(
            boolean allowed,
            int remaining,
            long resetEpochMs,
            long retryAfterSeconds) {

        public static RateLimitResult allowed(int remaining, long windowSeconds, long resetEpochMs) {
            return new RateLimitResult(true, remaining, resetEpochMs, 0);
        }

        public static RateLimitResult denied(int capacity, long resetEpochMs) {
            long retryAfter = Math.max(1,
                    (resetEpochMs - Instant.now().toEpochMilli()) / 1000);
            return new RateLimitResult(false, 0, resetEpochMs, retryAfter);
        }
    }
}