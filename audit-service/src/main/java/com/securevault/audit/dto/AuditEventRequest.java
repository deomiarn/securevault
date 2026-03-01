package com.securevault.audit.dto;

import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "Action is required")
    private AuditAction action;

    private UUID resourceId;

    private ResourceType resourceType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String ipAddress;

    private String userAgent;

    @NotNull(message = "Status is required")
    private EventStatus status;

    private String metadata;
}
