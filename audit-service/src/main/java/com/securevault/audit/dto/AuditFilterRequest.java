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
public class AuditFilterRequest {
    private UUID userId;
    private AuditAction action;
    private ResourceType resourceType;
    private EventStatus status;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String keyword;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
