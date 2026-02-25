package com.securevault.auth.service;

import com.securevault.auth.config.JwtProperties;
import com.securevault.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .claims(buildClaims(user))
                .claim("jti", UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(jwtProperties.getPrivateKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(jwtProperties.getPrivateKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    public long extractExpiration(String token) {
        Date exp = parseClaims(token).getExpiration();
        return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
    }

    private Claims buildClaims(User user) {
        return Jwts.claims()
                .add("email", user.getEmail())
                .add("role", user.getRole().name())
                .build();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
