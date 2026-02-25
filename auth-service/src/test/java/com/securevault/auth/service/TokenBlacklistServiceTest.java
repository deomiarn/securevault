package com.securevault.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void blacklist_StoresTokenWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist("jti-123", 300L);

        verify(valueOperations).set("blacklist:jti-123", "revoked", 300L, TimeUnit.SECONDS);
    }

    @Test
    void isBlacklisted_TokenExists_ReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:jti-123")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("jti-123")).isTrue();
    }

    @Test
    void isBlacklisted_TokenNotExists_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:jti-456")).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted("jti-456")).isFalse();
    }
}
