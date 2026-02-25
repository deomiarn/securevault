package com.securevault.gateway.filter;

import com.securevault.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtValidationFilterTest {

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;

    private JwtValidationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private StringWriter responseWriter;

    @BeforeAll
    static void loadKeys() throws Exception {
        privateKey = loadPrivateKey("private.pem");
        publicKey = loadPublicKey("public.pem");
    }

    @BeforeEach
    void setUp() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setPublicKey(publicKey);

        filter = new JwtValidationFilter(props);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void publicEndpoint_NoToken_Passes() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void protectedEndpoint_NoToken_Returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void protectedEndpoint_ValidToken_Passes() throws Exception {
        String token = createValidToken();
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void protectedEndpoint_ExpiredToken_Returns401() throws Exception {
        String token = createExpiredToken();
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void protectedEndpoint_InvalidSignature_Returns401() throws Exception {
        String token = createTokenWithWrongKey();
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void validToken_SetsUserHeaders() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = createTokenWithClaims(userId, "ADMIN");
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            HttpServletRequest wrapped = (HttpServletRequest) req;
            return userId.equals(wrapped.getHeader("X-User-Id"))
                    && "ADMIN".equals(wrapped.getHeader("X-User-Role"));
        }), eq(response));
    }

    @Test
    void validToken_StripsExistingUserHeaders() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = createTokenWithClaims(userId, "USER");
        when(request.getRequestURI()).thenReturn("/api/secrets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        // Client tries to inject their own X-User-Id header
        when(request.getHeader("X-User-Id")).thenReturn("attacker-id");
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            HttpServletRequest wrapped = (HttpServletRequest) req;
            // The wrapped request should use JWT claims, not the injected headers
            return userId.equals(wrapped.getHeader("X-User-Id"))
                    && "USER".equals(wrapped.getHeader("X-User-Role"));
        }), eq(response));
    }

    // --- Helper methods ---

    private String createValidToken() {
        return createTokenWithClaims(UUID.randomUUID().toString(), "USER");
    }

    private String createTokenWithClaims(String userId, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("email", "test@securevault.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(privateKey)
                .compact();
    }

    private String createExpiredToken() {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 1_000_000))
                .expiration(new Date(System.currentTimeMillis() - 500_000))
                .signWith(privateKey)
                .compact();
    }

    private String createTokenWithWrongKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var wrongKeyPair = keyGen.generateKeyPair();
        RSAPrivateKey wrongKey = (RSAPrivateKey) wrongKeyPair.getPrivate();

        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(wrongKey)
                .compact();
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
        try (InputStream is = JwtValidationFilterTest.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new IllegalStateException("PEM file not found: " + filename);
            return new String(is.readAllBytes());
        }
    }
}
