package com.securevault.audit.dto;

import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventResponse {
    private UUID id;
    private UUID userId;
    private AuditAction action;
    private ResourceType resourceType;
    private UUID resourceId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private EventStatus status;
    private String metadata;
    private LocalDateTime createdAt;
}
