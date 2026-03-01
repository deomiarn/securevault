package com.securevault.auth.service;

import com.securevault.auth.client.AuditClient;
import com.securevault.auth.dto.AuthResponse;
import com.securevault.auth.dto.LoginRequest;
import com.securevault.auth.dto.RegisterRequest;
import com.securevault.auth.entity.RefreshToken;
import com.securevault.auth.entity.User;
import com.securevault.auth.exception.EmailAlreadyExistsException;
import com.securevault.auth.exception.InvalidTokenException;
import com.securevault.auth.model.Role;
import com.securevault.auth.repository.RefreshTokenRepository;
import com.securevault.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditClient auditClient;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        auditClient.logAuth(user.getId(), "USER_REGISTERED", "SUCCESS",
                "User registered: " + user.getEmail(), null);

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .message("Registration successful")
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            userRepository.findByEmail(loginRequest.getEmail())
                    .ifPresent(user -> auditClient.logAuth(user.getId(), "USER_LOGIN_FAILED", "FAILURE",
                            "Login failed for: " + loginRequest.getEmail(), null));
            throw e;
        }

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow();

        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            return AuthResponse.builder()
                    .email(user.getEmail())
                    .totpRequired(true)
                    .message("2FA verification required")
                    .build();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        auditClient.logAuth(user.getId(), "USER_LOGIN", "SUCCESS",
                "User logged in: " + user.getEmail(), null);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Token Rotation: Revoke the old refresh token and issue a new one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken);

        auditClient.logAuth(user.getId(), "TOKEN_REFRESHED", "SUCCESS",
                "Token refreshed for: " + user.getEmail(), null);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenString, String accessToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String tokenId = jwtService.extractTokenId(accessToken);
        long expiration = jwtService.extractExpiration(accessToken);
        tokenBlacklistService.blacklist(tokenId, expiration);

        User user = refreshToken.getUser();
        auditClient.logAuth(user.getId(), "USER_LOGOUT", "SUCCESS",
                "User logged out: " + user.getEmail(), null);
    }

    public void saveRefreshToken(User user, String tokenString) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .build();
    }
}
