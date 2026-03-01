package com.securevault.audit.entity;

import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "userId"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_user_action", columnList = "userId, action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    private UUID resourceId;

    @Column(length = 500)
    private String description;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
