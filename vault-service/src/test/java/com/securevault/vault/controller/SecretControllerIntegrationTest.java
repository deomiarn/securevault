package com.securevault.vault.controller;

import com.securevault.vault.dto.CreateSecretRequest;
import com.securevault.vault.dto.ErrorResponse;
import com.securevault.vault.dto.SecretResponse;
import com.securevault.vault.dto.SecretSummaryResponse;
import com.securevault.vault.dto.ShareSecretRequest;
import com.securevault.vault.dto.SharedSecretResponse;
import com.securevault.vault.dto.UpdateSecretRequest;
import com.securevault.vault.model.Permission;
import com.securevault.vault.model.SecretType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestRestTemplate
class SecretControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void createSecret_Returns201() {
        CreateSecretRequest request = CreateSecretRequest.builder()
                .name("db-password")
                .value("s3cret!")
                .secretType(SecretType.PASSWORD)
                .build();

        ResponseEntity<SecretResponse> response = restTemplate.exchange(
                "/api/secrets",
                HttpMethod.POST,
                new HttpEntity<>(request, headersForUser(USER_A)),
                SecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("db-password");
        assertThat(response.getBody().getValue()).isEqualTo("s3cret!");
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void getSecret_Owner_Returns200WithDecryptedValue() {
        SecretResponse created = createSecretForUser(USER_A, "get-test", "my-value");

        ResponseEntity<SecretResponse> response = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_A)),
                SecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValue()).isEqualTo("my-value");
    }

    @Test
    void getSecret_OtherUser_Returns403() {
        SecretResponse created = createSecretForUser(USER_A, "private-secret", "hidden");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_B)),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAllSecrets_ReturnsOnlyOwnSecrets() {
        createSecretForUser(USER_A, "user-a-secret-" + UUID.randomUUID(), "val1");
        createSecretForUser(USER_B, "user-b-secret-" + UUID.randomUUID(), "val2");

        ResponseEntity<SecretSummaryResponse[]> response = restTemplate.exchange(
                "/api/secrets",
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_A)),
                SecretSummaryResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        for (SecretSummaryResponse summary : response.getBody()) {
            assertThat(summary.getName()).doesNotStartWith("user-b-secret-");
        }
    }

    @Test
    void updateSecret_Returns200() {
        SecretResponse created = createSecretForUser(USER_A, "to-update", "old-value");

        UpdateSecretRequest updateRequest = UpdateSecretRequest.builder()
                .name("updated-name")
                .value("new-value")
                .build();

        ResponseEntity<SecretResponse> response = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headersForUser(USER_A)),
                SecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("updated-name");
        assertThat(response.getBody().getValue()).isEqualTo("new-value");
    }

    @Test
    void deleteSecret_Returns204() {
        SecretResponse created = createSecretForUser(USER_A, "to-delete", "bye");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(headersForUser(USER_A)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it's gone
        ResponseEntity<ErrorResponse> getResponse = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_A)),
                ErrorResponse.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createSecret_InvalidBody_Returns400() {
        CreateSecretRequest request = CreateSecretRequest.builder().build();

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/secrets",
                HttpMethod.POST,
                new HttpEntity<>(request, headersForUser(USER_A)),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shareSecret_Returns201() {
        SecretResponse created = createSecretForUser(USER_A, "to-share", "shared-val");

        ShareSecretRequest shareRequest = ShareSecretRequest.builder()
                .sharedWithUserId(USER_B)
                .permission(Permission.READ)
                .build();

        ResponseEntity<SharedSecretResponse> response = restTemplate.exchange(
                "/api/secrets/" + created.getId() + "/shares",
                HttpMethod.POST,
                new HttpEntity<>(shareRequest, headersForUser(USER_A)),
                SharedSecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSharedWithUserId()).isEqualTo(USER_B);
        assertThat(response.getBody().getPermission()).isEqualTo(Permission.READ);
    }

    @Test
    void getSharedSecret_Returns200() {
        SecretResponse created = createSecretForUser(USER_A, "shared-read", "read-me");

        ShareSecretRequest shareRequest = ShareSecretRequest.builder()
                .sharedWithUserId(USER_B)
                .permission(Permission.READ)
                .build();
        restTemplate.exchange(
                "/api/secrets/" + created.getId() + "/shares",
                HttpMethod.POST,
                new HttpEntity<>(shareRequest, headersForUser(USER_A)),
                SharedSecretResponse.class
        );

        ResponseEntity<SecretResponse> response = restTemplate.exchange(
                "/api/secrets/" + created.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_B)),
                SecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValue()).isEqualTo("read-me");
    }

    @Test
    void fullFlow_Create_Share_Read_Revoke() {
        // 1. Create a secret
        SecretResponse created = createSecretForUser(USER_A, "flow-test", "flow-value");
        UUID secretId = created.getId();

        // 2. Share with User B (READ)
        ShareSecretRequest shareRequest = ShareSecretRequest.builder()
                .sharedWithUserId(USER_B)
                .permission(Permission.READ)
                .build();
        ResponseEntity<SharedSecretResponse> shareResponse = restTemplate.exchange(
                "/api/secrets/" + secretId + "/shares",
                HttpMethod.POST,
                new HttpEntity<>(shareRequest, headersForUser(USER_A)),
                SharedSecretResponse.class
        );
        assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID shareId = shareResponse.getBody().getId();

        // 3. User B can read it
        ResponseEntity<SecretResponse> readResponse = restTemplate.exchange(
                "/api/secrets/" + secretId,
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_B)),
                SecretResponse.class
        );
        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readResponse.getBody().getValue()).isEqualTo("flow-value");

        // 4. Revoke the share
        ResponseEntity<Void> revokeResponse = restTemplate.exchange(
                "/api/secrets/" + secretId + "/shares/" + shareId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersForUser(USER_A)),
                Void.class
        );
        assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. User B can no longer access it
        ResponseEntity<ErrorResponse> deniedResponse = restTemplate.exchange(
                "/api/secrets/" + secretId,
                HttpMethod.GET,
                new HttpEntity<>(headersForUser(USER_B)),
                ErrorResponse.class
        );
        assertThat(deniedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- Helpers ---

    private HttpHeaders headersForUser(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private SecretResponse createSecretForUser(UUID userId, String name, String value) {
        CreateSecretRequest request = CreateSecretRequest.builder()
                .name(name)
                .value(value)
                .secretType(SecretType.PASSWORD)
                .build();

        ResponseEntity<SecretResponse> response = restTemplate.exchange(
                "/api/secrets",
                HttpMethod.POST,
                new HttpEntity<>(request, headersForUser(userId)),
                SecretResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
