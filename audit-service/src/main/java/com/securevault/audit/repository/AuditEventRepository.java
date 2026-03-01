package com.securevault.audit.repository;

import com.securevault.audit.entity.AuditEvent;
import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>,
        JpaSpecificationExecutor<AuditEvent> {

    Page<AuditEvent> findAllByUserId(UUID userId, Pageable pageable);

    Page<AuditEvent> findAllByAction(AuditAction action, Pageable pageable);

    List<AuditEvent> findAllByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    Page<AuditEvent> findAllByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
