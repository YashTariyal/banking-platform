package com.banking.card.web;

import com.banking.card.service.NotificationService;
import com.banking.card.web.dto.CreateNotificationRequest;
import com.banking.card.web.dto.NotificationResponse;
import com.banking.card.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/notifications")
@Tag(name = "Card Notifications", description = "Card notification operations")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create notification",
            description = "Creates and sends a notification for a card via the specified channel (email, SMS, push)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public NotificationResponse createNotification(
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        return notificationService.createNotification(cardId, request);
    }

    @GetMapping
    @Operation(
            summary = "Get notifications",
            description = "Retrieves notification history for a card with pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public PageResponse<NotificationResponse> getNotifications(
            @PathVariable UUID cardId,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(cardId, page, size);
    }
}

