package com.securevault.auth.service;

import com.securevault.auth.config.JwtProperties;
import com.securevault.auth.entity.User;
import com.securevault.auth.model.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    private JwtService jwtService;
    private User testUser;

    @BeforeAll
    static void loadKeys() throws Exception {
        privateKey = loadPrivateKey("private.pem");
        publicKey = loadPublicKey("public.pem");
    }

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setPrivateKey(privateKey);
        props.setPublicKey(publicKey);
        props.setAccessTokenExpiration(900_000L);
        props.setRefreshTokenExpiration(604_800_000L);

        jwtService = new JwtService(props);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@securevault.com")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateAccessToken_ContainsCorrectClaims() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@securevault.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.extractTokenId(token)).isNotBlank();
    }

    @Test
    void generateRefreshToken_ContainsUserId() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setPrivateKey(privateKey);
        expiredProps.setPublicKey(publicKey);
        expiredProps.setAccessTokenExpiration(0L);
        expiredProps.setRefreshTokenExpiration(0L);

        JwtService expiredJwtService = new JwtService(expiredProps);
        String token = expiredJwtService.generateAccessToken(testUser);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @Test
    void extractUserId_ReturnsCorrectId() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
    }

    @Test
    void extractEmail_ReturnsCorrectEmail() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@securevault.com");
    }

    @Test
    void extractRole_ReturnsCorrectRole() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void extractTokenId_ReturnsJti() {
        String token = jwtService.generateAccessToken(testUser);
        String jti = jwtService.extractTokenId(token);
        assertThat(jti).isNotNull();
        // JTI should be a valid UUID
        assertThat(UUID.fromString(jti)).isNotNull();
    }

    private static RSAPrivateKey loadPrivateKey(String filename) throws Exception {
        String pem = readPemFile(filename);
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static RSAPublicKey loadPublicKey(String filename) throws Exception {
        String pem = readPemFile(filename);
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String readPemFile(String filename) throws Exception {
        try (InputStream is = JwtServiceTest.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new IllegalStateException("PEM file not found: " + filename);
            return new String(is.readAllBytes());
        }
    }
}
