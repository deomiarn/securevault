package com.securevault.audit.controller;

import com.securevault.audit.entity.AuditEvent;
import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import com.securevault.audit.repository.AuditEventRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuditEventControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("securevault")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private UUID testUserId;
    private UUID testResourceId;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        testUserId = UUID.randomUUID();
        testResourceId = UUID.randomUUID();
    }

    private String eventJson(UUID userId, String action, String status, String description) {
        return """
                {
                    "userId": %s,
                    "action": "%s",
                    "resourceType": "USER",
                    "resourceId": "%s",
                    "description": "%s",
                    "ipAddress": "192.168.1.1",
                    "userAgent": "TestAgent/1.0",
                    "status": "%s",
                    "metadata": "{\\"test\\": true}"
                }
                """.formatted(
                userId != null ? "\"" + userId + "\"" : "null",
                action, testResourceId, description, status);
    }

    private void createTestEvent(UUID userId, AuditAction action, EventStatus status, String description) {
        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.USER)
                .resourceId(testResourceId)
                .description(description)
                .ipAddress("192.168.1.1")
                .userAgent("TestAgent/1.0")
                .status(status)
                .build();
        auditEventRepository.save(event);
    }

    @Test
    void recordEvent_ValidRequest_Returns201() throws Exception {
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(testUserId, "USER_LOGIN", "SUCCESS", "User logged in")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                .andExpect(jsonPath("$.action", is("USER_LOGIN")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.description", is("User logged in")));
    }

    @Test
    void recordEvent_MissingAction_Returns400() throws Exception {
        String json = """
                {
                    "userId": "%s",
                    "status": "SUCCESS",
                    "description": "Some event"
                }
                """.formatted(testUserId);

        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordEvent_MissingStatus_Returns400() throws Exception {
        String json = """
                {
                    "userId": "%s",
                    "action": "USER_LOGIN",
                    "description": "Some event"
                }
                """.formatted(testUserId);

        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_AdminRole_Returns200() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "Admin query test");

        mockMvc.perform(get("/api/audit/events")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getEvents_ManagerRole_Returns200() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "Manager query test");

        mockMvc.perform(get("/api/audit/events")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getEvents_UserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEvents_FilterByAction_ReturnsFiltered() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "Login event");
        createTestEvent(testUserId, AuditAction.SECRET_CREATED, EventStatus.SUCCESS, "Secret created");

        mockMvc.perform(get("/api/audit/events")
                        .header("X-User-Role", "ADMIN")
                        .param("action", "USER_LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action", is("USER_LOGIN")));
    }

    @Test
    void getEventsForUser_OwnEvents_Returns200() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "Own user event");

        mockMvc.perform(get("/api/audit/events/user/{userId}", testUserId)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getEventsForUser_OtherUser_NonAdmin_Returns403() throws Exception {
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/audit/events/user/{userId}", otherUserId)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEventsForUser_OtherUser_Admin_Returns200() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        createTestEvent(otherUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "Other user event");

        mockMvc.perform(get("/api/audit/events/user/{userId}", otherUserId)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getEventsForResource_AdminRole_Returns200() throws Exception {
        createTestEvent(testUserId, AuditAction.SECRET_CREATED, EventStatus.SUCCESS, "Resource event");

        mockMvc.perform(get("/api/audit/events/resource/{type}/{resourceId}", "USER", testResourceId)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getEventsForResource_UserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/audit/events/resource/{type}/{resourceId}", "USER", testResourceId)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportCsv_AdminRole_ReturnsCsv() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "CSV export test");

        mockMvc.perform(get("/api/audit/events/export/csv")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit-events.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(result -> {
                    String csv = result.getResponse().getContentAsString();
                    String[] lines = csv.split("\n");
                    org.assertj.core.api.Assertions.assertThat(lines[0])
                            .isEqualTo("id,userId,action,resourceType,resourceId,description,ipAddress,status,createdAt");
                    org.assertj.core.api.Assertions.assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
                    org.assertj.core.api.Assertions.assertThat(lines[1]).contains("USER_LOGIN");
                });
    }

    @Test
    void exportCsv_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/audit/events/export/csv")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportJson_AdminRole_ReturnsJson() throws Exception {
        createTestEvent(testUserId, AuditAction.USER_LOGIN, EventStatus.SUCCESS, "JSON export test");

        mockMvc.perform(get("/api/audit/events/export/json")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].action", is("USER_LOGIN")));
    }

    @Test
    void fullFlow_RecordAndQuery() throws Exception {
        // 1. Record an event via API
        mockMvc.perform(post("/api/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(testUserId, "SECRET_CREATED", "SUCCESS", "Full flow test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));

        // 2. Query events and verify the created event is in the results
        mockMvc.perform(get("/api/audit/events")
                        .header("X-User-Role", "ADMIN")
                        .param("action", "SECRET_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action", is("SECRET_CREATED")))
                .andExpect(jsonPath("$.content[0].description", is("Full flow test")));

        // 3. Query by user
        mockMvc.perform(get("/api/audit/events/user/{userId}", testUserId)
                        .header("X-User-Id", testUserId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 4. Export as CSV
        mockMvc.perform(get("/api/audit/events/export/csv")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String csv = result.getResponse().getContentAsString();
                    org.assertj.core.api.Assertions.assertThat(csv).contains("SECRET_CREATED");
                    org.assertj.core.api.Assertions.assertThat(csv).contains("Full flow test");
                });
    }
}
