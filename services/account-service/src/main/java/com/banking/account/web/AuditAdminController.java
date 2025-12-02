package com.banking.account.web;

import com.banking.account.audit.EventAuditLog;
import com.banking.account.audit.EventAuditLogRepository;
import com.banking.account.audit.EventDirection;
import com.banking.account.audit.EventStatus;
import com.banking.account.web.dto.EventAuditLogResponse;
import com.banking.account.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit/events")
@Tag(name = "Audit Admin", description = "Administrative access to event audit logs")
public class AuditAdminController {

    private static final int MAX_PAGE_SIZE = 200;

    private final EventAuditLogRepository repository;

    public AuditAdminController(EventAuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(
            summary = "List event audit logs",
            description = "Pages through event audit logs with optional filters by status, direction and topic substring."
    )
    @ApiResponse(responseCode = "200", description = "Page of audit logs",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventAuditLogResponse.class))))
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('accounts.admin')")
    public PageResponse<EventAuditLogResponse> listAuditLogs(
            @Parameter(description = "Filter by status (PENDING, SUCCESS, FAILED)")
            @RequestParam(required = false) EventStatus status,
            @Parameter(description = "Filter by direction (PUBLISH, CONSUME)")
            @RequestParam(required = false) EventDirection direction,
            @Parameter(description = "Filter by topic containing this value (case-insensitive)")
            @RequestParam(required = false) String topic,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 200)", example = "50")
            @RequestParam(defaultValue = "50") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);

        Page<EventAuditLog> resultPage;
        if (topic != null && !topic.isBlank()) {
            resultPage = repository.findByTopicContainingIgnoreCase(topic, pageable);
        } else if (status != null && direction != null) {
            resultPage = repository.findByStatusAndDirection(status, direction, pageable);
        } else if (status != null) {
            resultPage = repository.findByStatus(status, pageable);
        } else if (direction != null) {
            resultPage = repository.findByDirection(direction, pageable);
        } else {
            resultPage = repository.findAll(pageable);
        }

        return PageResponse.from(resultPage.map(EventAuditLogMapper::toResponse));
    }
}


