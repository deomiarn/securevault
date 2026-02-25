package com.securevault.gateway.filter;

import com.securevault.gateway.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;
    private RateLimitProperties rateLimitProperties;

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setRequestsPerMinute(100);
        rateLimitProperties.setLoginRequestsPerMinute(10);
        rateLimitFilter = new RateLimitFilter(redisTemplate, rateLimitProperties);
    }

    @Test
    void underLimit_RequestPasses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void overLimit_Returns429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(101L);

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("Rate limit exceeded");
    }

    @Test
    void loginEndpoint_StricterLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 11 > loginRequestsPerMinute (10) → should be blocked
        when(valueOperations.increment(anyString())).thenReturn(11L);

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void setsRateLimitHeaders() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("100"));
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("95"));
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    void firstRequest_SetsExpire() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // count == 1 means first request → TTL should be set
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).expire(anyString(), eq(60L), any());
        verify(filterChain).doFilter(request, response);
    }
}
