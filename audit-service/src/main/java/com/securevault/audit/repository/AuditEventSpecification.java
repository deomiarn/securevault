package com.securevault.audit.repository;

import com.securevault.audit.entity.AuditEvent;
import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditEventSpecification {

    public static Specification<AuditEvent> hasUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditEvent> hasAction(AuditAction action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditEvent> hasResourceType(ResourceType resourceType) {
        return (root, query, cb) -> cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<AuditEvent> hasStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<AuditEvent> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }

    public static Specification<AuditEvent> containsDescription(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("description")),
                "%" + keyword.toLowerCase() + "%");
    }
}
