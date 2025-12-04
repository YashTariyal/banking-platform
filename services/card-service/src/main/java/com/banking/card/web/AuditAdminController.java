package com.banking.card.web;

import com.banking.card.audit.EventAuditLogRepository;
import com.banking.card.audit.EventDirection;
import com.banking.card.audit.EventStatus;
import com.banking.card.web.dto.EventAuditLogResponse;
import com.banking.card.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Card Audit Admin", description = "Administrative APIs for card-service event audit logs")
public class AuditAdminController {

    private final EventAuditLogRepository repository;

    public AuditAdminController(EventAuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(
            summary = "List event audit logs",
            description = "Lists event audit logs with optional filters for status, direction and topic."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.admin')")
    public PageResponse<EventAuditLogResponse> listAuditLogs(
            @Parameter(description = "Filter by status (PENDING, SUCCESS, FAILED)")
            @RequestParam(required = false) EventStatus status,
            @Parameter(description = "Filter by direction (PUBLISH, CONSUME)")
            @RequestParam(required = false) EventDirection direction,
            @Parameter(description = "Filter by topic, substring match (case-insensitive)")
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.banking.card.audit.EventAuditLog> logs;

        if (topic != null && !topic.isBlank()) {
            logs = repository.findByTopicContainingIgnoreCase(topic, pageable);
        } else if (status != null && direction != null) {
            logs = repository.findByStatusAndDirection(status, direction, pageable);
        } else if (status != null) {
            logs = repository.findByStatus(status, pageable);
        } else if (direction != null) {
            logs = repository.findByDirection(direction, pageable);
        } else {
            logs = repository.findAll(pageable);
        }

        return PageResponse.from(logs.map(EventAuditLogMapper::toResponse));
    }
}


