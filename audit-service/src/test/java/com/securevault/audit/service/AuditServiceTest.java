package com.securevault.audit.service;

import com.securevault.audit.dto.AuditEventRequest;
import com.securevault.audit.dto.AuditEventResponse;
import com.securevault.audit.dto.AuditFilterRequest;
import com.securevault.audit.dto.PageResponse;
import com.securevault.audit.entity.AuditEvent;
import com.securevault.audit.model.AuditAction;
import com.securevault.audit.model.EventStatus;
import com.securevault.audit.model.ResourceType;
import com.securevault.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditEvent testEvent;
    private AuditEventRequest testRequest;
    private UUID userId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        resourceId = UUID.randomUUID();

        testEvent = AuditEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(AuditAction.USER_LOGIN)
                .resourceType(ResourceType.USER)
                .resourceId(resourceId)
                .description("User logged in successfully")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .status(EventStatus.SUCCESS)
                .metadata("{\"browser\":\"Chrome\"}")
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = AuditEventRequest.builder()
                .userId(userId)
                .action(AuditAction.USER_LOGIN)
                .resourceType(ResourceType.USER)
                .resourceId(resourceId)
                .description("User logged in successfully")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .status(EventStatus.SUCCESS)
                .metadata("{\"browser\":\"Chrome\"}")
                .build();
    }

    @Test
    void recordEvent_Success() {
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        AuditEventResponse response = auditService.recordEvent(testRequest);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAction()).isEqualTo(AuditAction.USER_LOGIN);
        assertThat(response.getResourceType()).isEqualTo(ResourceType.USER);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getDescription()).isEqualTo("User logged in successfully");
        assertThat(response.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(response.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(response.getStatus()).isEqualTo(EventStatus.SUCCESS);
        assertThat(response.getMetadata()).isEqualTo("{\"browser\":\"Chrome\"}");
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void recordEvent_NullUserId_Accepted() {
        AuditEventRequest request = AuditEventRequest.builder()
                .userId(null)
                .action(AuditAction.USER_LOGIN_FAILED)
                .status(EventStatus.FAILURE)
                .description("Failed login attempt")
                .build();

        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        AuditEventResponse response = auditService.recordEvent(request);

        assertThat(response.getUserId()).isNull();
        assertThat(response.getAction()).isEqualTo(AuditAction.USER_LOGIN_FAILED);
        assertThat(response.getStatus()).isEqualTo(EventStatus.FAILURE);
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_NoFilter_ReturnsAll() {
        AuditFilterRequest filter = AuditFilterRequest.builder().build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_FilterByUser_ReturnsCorrectResults() {
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .userId(userId)
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getUserId()).isEqualTo(userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_FilterByAction_ReturnsCorrectResults() {
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .action(AuditAction.USER_LOGIN)
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getAction()).isEqualTo(AuditAction.USER_LOGIN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_FilterByDateRange_ReturnsCorrectResults() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .fromDate(from)
                .toDate(to)
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_FilterByKeyword_ReturnsCorrectResults() {
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .keyword("logged in")
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_CombinedFilters_ReturnsCorrectResults() {
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .userId(userId)
                .action(AuditAction.USER_LOGIN)
                .status(EventStatus.SUCCESS)
                .keyword("logged")
                .fromDate(LocalDateTime.now().minusDays(1))
                .toDate(LocalDateTime.now().plusDays(1))
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEvents(filter);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getUserId()).isEqualTo(userId);
        assertThat(response.getContent().getFirst().getAction()).isEqualTo(AuditAction.USER_LOGIN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_PaginationMaxSize_CappedAt100() {
        AuditFilterRequest filter = AuditFilterRequest.builder()
                .size(200)
                .build();
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    assertThat(pageable.getPageSize()).isEqualTo(100);
                    return page;
                });

        auditService.getEvents(filter);
    }

    @Test
    void getEventsForUser_ReturnsUserEvents() {
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));

        when(auditEventRepository.findAllByUserId(any(UUID.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditEventResponse> response = auditService.getEventsForUser(userId,
                Pageable.ofSize(20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getUserId()).isEqualTo(userId);
        verify(auditEventRepository).findAllByUserId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void getEventsForResource_ReturnsResourceEvents() {
        when(auditEventRepository.findAllByResourceTypeAndResourceId(ResourceType.USER, resourceId))
                .thenReturn(List.of(testEvent));

        List<AuditEventResponse> response = auditService.getEventsForResource(ResourceType.USER, resourceId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getResourceType()).isEqualTo(ResourceType.USER);
        assertThat(response.getFirst().getResourceId()).isEqualTo(resourceId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportEventsAsCsv_CorrectFormat() {
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));
        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditFilterRequest filter = AuditFilterRequest.builder().build();
        String csv = auditService.exportEventsAsCsv(filter);

        String[] lines = csv.split("\n");
        assertThat(lines[0]).isEqualTo("id,userId,action,resourceType,resourceId,description,ipAddress,status,createdAt");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).contains("USER_LOGIN");
        assertThat(lines[1]).contains("SUCCESS");
        assertThat(lines[1]).contains("192.168.1.1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportEventsAsCsv_EscapesCommasInDescription() {
        testEvent.setDescription("Login from New York, USA");
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));
        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditFilterRequest filter = AuditFilterRequest.builder().build();
        String csv = auditService.exportEventsAsCsv(filter);

        assertThat(csv).contains("\"Login from New York, USA\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportEventsAsCsv_EscapesQuotesInDescription() {
        testEvent.setDescription("User said \"hello\"");
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));
        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditFilterRequest filter = AuditFilterRequest.builder().build();
        String csv = auditService.exportEventsAsCsv(filter);

        assertThat(csv).contains("\"User said \"\"hello\"\"\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportEventsAsJson_ReturnsAllEvents() {
        Page<AuditEvent> page = new PageImpl<>(List.of(testEvent));
        when(auditEventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditFilterRequest filter = AuditFilterRequest.builder().build();
        List<AuditEventResponse> response = auditService.exportEventsAsJson(filter);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getAction()).isEqualTo(AuditAction.USER_LOGIN);
        assertThat(response.getFirst().getUserId()).isEqualTo(userId);
    }
}
