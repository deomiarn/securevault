package com.securevault.audit.service;

import com.securevault.audit.dto.AuditEventRequest;
import com.securevault.audit.dto.AuditEventResponse;
import com.securevault.audit.dto.AuditFilterRequest;
import com.securevault.audit.dto.PageResponse;
import com.securevault.audit.entity.AuditEvent;
import com.securevault.audit.model.ResourceType;
import com.securevault.audit.repository.AuditEventRepository;
import com.securevault.audit.repository.AuditEventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditEventResponse recordEvent(AuditEventRequest request) {
        AuditEvent event = AuditEvent.builder()
                .userId(request.getUserId())
                .action(request.getAction())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .description(request.getDescription())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .status(request.getStatus())
                .metadata(request.getMetadata())
                .build();

        AuditEvent saved = auditEventRepository.save(event);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> getEvents(AuditFilterRequest filter) {
        Specification<AuditEvent> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                Math.min(filter.getSize(), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditEvent> page = auditEventRepository.findAll(spec, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> getEventsForUser(UUID userId, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.findAllByUserId(userId, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getEventsForResource(ResourceType type, UUID resourceId) {
        return auditEventRepository.findAllByResourceTypeAndResourceId(type, resourceId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> exportEventsAsJson(AuditFilterRequest filter) {
        Specification<AuditEvent> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditEventRepository.findAll(spec, pageable)
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportEventsAsCsv(AuditFilterRequest filter) {
        List<AuditEventResponse> events = exportEventsAsJson(filter);
        StringBuilder csv = new StringBuilder();
        csv.append("id,userId,action,resourceType,resourceId,description,ipAddress,status,createdAt\n");

        for (AuditEventResponse event : events) {
            csv.append(event.getId()).append(",");
            csv.append(event.getUserId()).append(",");
            csv.append(event.getAction()).append(",");
            csv.append(event.getResourceType()).append(",");
            csv.append(event.getResourceId()).append(",");
            csv.append(escapeCsv(event.getDescription())).append(",");
            csv.append(event.getIpAddress()).append(",");
            csv.append(event.getStatus()).append(",");
            csv.append(event.getCreatedAt()).append("\n");
        }

        return csv.toString();
    }

    private Specification<AuditEvent> buildSpecification(AuditFilterRequest filter) {
        Specification<AuditEvent> spec = Specification.where(
                (root, query, cb) -> cb.conjunction()
        );

        if (filter.getUserId() != null) {
            spec = spec.and(AuditEventSpecification.hasUserId(filter.getUserId()));
        }
        if (filter.getAction() != null) {
            spec = spec.and(AuditEventSpecification.hasAction(filter.getAction()));
        }
        if (filter.getResourceType() != null) {
            spec = spec.and(AuditEventSpecification.hasResourceType(filter.getResourceType()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and(AuditEventSpecification.hasStatus(filter.getStatus()));
        }
        if (filter.getFromDate() != null && filter.getToDate() != null) {
            spec = spec.and(AuditEventSpecification.createdBetween(filter.getFromDate(), filter.getToDate()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(AuditEventSpecification.containsDescription(filter.getKeyword()));
        }

        return spec;
    }

    // Custom PageResponse class to avoid exposing Spring's Page directly with unnecessary details
    private PageResponse<AuditEventResponse> mapToPageResponse(Page<AuditEvent> page) {
        List<AuditEventResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<AuditEventResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private AuditEventResponse mapToResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .description(event.getDescription())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .status(event.getStatus())
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
