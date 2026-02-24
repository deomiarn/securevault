package com.securevault.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public void blacklist(String tokenId, long expirationInSeconds) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + tokenId,
                "revoked",
                expirationInSeconds,
                TimeUnit.SECONDS
        );
    }

    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId));
    }
}
