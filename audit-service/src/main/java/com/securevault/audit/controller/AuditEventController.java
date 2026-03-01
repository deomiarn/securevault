package com.securevault.audit.controller;

import com.securevault.audit.dto.AuditEventRequest;
import com.securevault.audit.dto.AuditEventResponse;
import com.securevault.audit.dto.AuditFilterRequest;
import com.securevault.audit.dto.PageResponse;
import com.securevault.audit.model.ResourceType;
import com.securevault.audit.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditEventController {
    private final AuditService auditService;

    @PostMapping("/events")
    public ResponseEntity<AuditEventResponse> recordEvent(
            @Valid @RequestBody AuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditService.recordEvent(request));
    }

    @GetMapping("/events")
    public ResponseEntity<PageResponse<AuditEventResponse>> getEvents(
            @RequestHeader("X-User-Role") String role,
            @ModelAttribute AuditFilterRequest filter) {
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.getEvents(filter));
    }

    @GetMapping("/events/user/{userId}")
    public ResponseEntity<PageResponse<AuditEventResponse>> getEventsForUser(
            @RequestHeader("X-User-Id") UUID currentUserId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!"ADMIN".equals(role) && !currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.getEventsForUser(userId,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/events/resource/{type}/{resourceId}")
    public ResponseEntity<List<AuditEventResponse>> getEventsForResource(
            @RequestHeader("X-User-Role") String role,
            @PathVariable ResourceType type,
            @PathVariable UUID resourceId) {
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.getEventsForResource(type, resourceId));
    }

    @GetMapping("/events/export/csv")
    public ResponseEntity<String> exportCsv(
            @RequestHeader("X-User-Role") String role,
            @ModelAttribute AuditFilterRequest filter) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String csv = auditService.exportEventsAsCsv(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-events.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(csv);
    }

    @GetMapping("/events/export/json")
    public ResponseEntity<List<AuditEventResponse>> exportJson(
            @RequestHeader("X-User-Role") String role,
            @ModelAttribute AuditFilterRequest filter) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.exportEventsAsJson(filter));
    }
}
