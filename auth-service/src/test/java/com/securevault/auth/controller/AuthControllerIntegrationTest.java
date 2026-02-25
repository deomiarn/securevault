package com.securevault.auth.controller;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("securevault")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    private String uniqueEmail;

    @BeforeEach
    void setUp() {
        uniqueEmail = "integration-" + System.nanoTime() + "@test.com";
    }

    private String registerJson(String email) {
        return """
                {"email":"%s","password":"password123","firstName":"Integration","lastName":"Test"}
                """.formatted(email);
    }

    private String loginJson(String email) {
        return """
                {"email":"%s","password":"password123"}
                """.formatted(email);
    }

    private String refreshJson(String token) {
        return """
                {"refreshToken":"%s"}
                """.formatted(token);
    }

    @Test
    void register_ValidRequest_Returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(uniqueEmail)))
                .andExpect(jsonPath("$.message", is("Registration successful")));
    }

    @Test
    void register_DuplicateEmail_Returns409() throws Exception {
        String email = "dup-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidCredentials_ReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        String email = "bad-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrongpassword"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokens() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String refreshToken = extractJsonField(body, "refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    void logout_ValidToken_Returns200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = extractJsonField(body, "accessToken");
        String refreshToken = extractJsonField(body, "refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logout successful")));
    }

    @Test
    void fullFlow_Register_Login_Refresh_Logout() throws Exception {
        // 1. Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail)))
                .andExpect(status().isCreated());

        // 2. Login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        String refreshToken = extractJsonField(loginBody, "refreshToken");

        // 3. Refresh
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andReturn();

        String refreshBody = refreshResult.getResponse().getContentAsString();
        String newAccessToken = extractJsonField(refreshBody, "accessToken");
        String newRefreshToken = extractJsonField(refreshBody, "refreshToken");

        // 4. Logout with new tokens
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(newRefreshToken)))
                .andExpect(status().isOk());

        // 5. Old refresh token should no longer work
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private String extractJsonField(String json, String field) {
        // Simple extraction without Jackson dependency
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
