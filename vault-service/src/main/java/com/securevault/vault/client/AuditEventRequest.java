package com.securevault.vault.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventRequest {

    private UUID userId;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String metadata;
}
