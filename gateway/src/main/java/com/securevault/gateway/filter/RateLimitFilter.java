package com.securevault.gateway.filter;

import com.securevault.gateway.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    private static final String RATE_LIMIT_PREFIX = "rate-limit:";

    private static final List<String> LOGIN_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/2fa/verify-login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        boolean isLoginPath = isLoginPath(path);

        // Determine the appropriate rate limit based on the endpoint: Login = 10/minute, Rest = 100/minute
        int limit = isLoginPath
                ? rateLimitProperties.getLoginRequestsPerMinute()
                : rateLimitProperties.getRequestsPerMinute();

        // Redis key format: rate-limit:{type}:{clientIp}:{currentMinute}
        long currentMinute = Instant.now().getEpochSecond() / 60;
        String prefix = isLoginPath ? "login:" : "general:";
        String key = RATE_LIMIT_PREFIX + prefix + clientIp + ":" + currentMinute;

        Long count = redisTemplate.opsForValue().increment(key);

        // Set expiration for the key to ensure it resets after the time window
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        long currentCount = count != null ? count : 1;

        long remaining = Math.max(0, limit - currentCount);
        long resetTime = (currentMinute + 1) * 60; // next minute in epoch seconds
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));

        if (currentCount > limit) {
            long retryAfter = resetTime - Instant.now().getEpochSecond();
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\": \"Rate limit exceeded. Try again in " + Math.max(1, retryAfter) + " seconds.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For Header validation (if behind a proxy/load balancer)
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // First IP in the list is the original client IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLoginPath(String path) {
        return LOGIN_PATHS.stream().anyMatch(path::startsWith);
    }
}
