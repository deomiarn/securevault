package com.securevault.auth.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@securevault.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .totpEnabled(false)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@securevault.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@securevault.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getEmail()).isEqualTo("test@securevault.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getMessage()).isEqualTo("Registration successful");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@securevault.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void login_Success_ReturnsTokens() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@securevault.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("test@securevault.com");
        assertThat(response.getMessage()).isEqualTo("Login successful");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_TotpEnabled_ReturnsTotpRequired() {
        testUser.setTotpEnabled(true);
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("test@securevault.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getTotpRequired()).isTrue();
        assertThat(response.getMessage()).isEqualTo("2FA verification required");
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokens() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("old-refresh-token")
                .user(testUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh("old-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(refreshToken.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void refresh_RevokedToken_ThrowsException() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("revoked-token")
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void refresh_ExpiredToken_ThrowsException() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("expired-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token has expired");
    }

    @Test
    void logout_Success_RevokesTokenAndBlacklists() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.extractTokenId("access-token")).thenReturn("jti-123");
        when(jwtService.extractExpiration("access-token")).thenReturn(300L);

        authService.logout("refresh-token", "access-token");

        assertThat(refreshToken.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
        verify(tokenBlacklistService).blacklist("jti-123", 300L);
    }
}
