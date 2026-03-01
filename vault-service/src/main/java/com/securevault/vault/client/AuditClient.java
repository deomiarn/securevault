package com.securevault.vault.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@Slf4j
public class AuditClient {

    private final RestClient restClient;

    public AuditClient(@Value("${audit-service.url}") String auditServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(auditServiceUrl)
                .build();
    }

    @Async
    public void sendEvent(AuditEventRequest request) {
        try {
            restClient.post()
                    .uri("/api/audit/events")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send audit event: {}", e.getMessage());
        }
    }

    public void logSecret(UUID userId, UUID secretId, String action, String status, String description) {
        sendEvent(AuditEventRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType("SECRET")
                .resourceId(secretId)
                .status(status)
                .description(description)
                .build());
    }

    public void logFolder(UUID userId, UUID folderId, String action, String status, String description) {
        sendEvent(AuditEventRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType("FOLDER")
                .resourceId(folderId)
                .status(status)
                .description(description)
                .build());
    }

    public void logShare(UUID userId, UUID secretId, String action, String status, String description) {
        sendEvent(AuditEventRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType("SHARE")
                .resourceId(secretId)
                .status(status)
                .description(description)
                .build());
    }
}
