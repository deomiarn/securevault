package com.securevault.gateway.filter;

import com.securevault.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// This filter checks for the presence of a valid JWT in the Authorization header for protected endpoints.
@Component
@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/2fa/verify-login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return;
        }

        // Token extraction and validation
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtProperties.getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Extend the request with user information for downstream services
            HttpServletRequest wrappedRequest = wrapRequestWithUserHeaders(request, userId, role);
            filterChain.doFilter(wrappedRequest, response);

        } catch (JwtException | IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }

    private HttpServletRequest wrapRequestWithUserHeaders(HttpServletRequest request,
                                                          String userId,
                                                          String role) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) return userId;
                if ("X-User-Role".equalsIgnoreCase(name)) return role;
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equalsIgnoreCase(name))
                    return Collections.enumeration(List.of(userId));
                if ("X-User-Role".equalsIgnoreCase(name))
                    return Collections.enumeration(List.of(role));
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new java.util.ArrayList<>(
                        Collections.list(super.getHeaderNames()));
                if (!names.contains("X-User-Id")) names.add("X-User-Id");
                if (!names.contains("X-User-Role")) names.add("X-User-Role");
                return Collections.enumeration(names);
            }
        };
    }
}
