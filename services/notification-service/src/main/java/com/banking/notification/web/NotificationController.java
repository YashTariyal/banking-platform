package com.banking.notification.web;

import com.banking.notification.domain.Notification;
import com.banking.notification.domain.Notification.*;
import com.banking.notification.domain.NotificationPreference;
import com.banking.notification.service.NotificationService;
import com.banking.notification.service.NotificationService.NotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Send a notification")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        Notification notification = notificationService.createAndSendNotification(new NotificationRequest(
                request.customerId(),
                request.userId(),
                request.channel(),
                request.type(),
                request.recipient(),
                request.subject(),
                request.content(),
                request.priority(),
                request.templateName(),
                null
        ));
        return ResponseEntity.ok(toResponse(notification));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer notifications")
    public ResponseEntity<Page<NotificationResponse>> getCustomerNotifications(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationService.getCustomerNotifications(customerId, pageRequest);
        return ResponseEntity.ok(notifications.map(this::toResponse));
    }

    @GetMapping("/customer/{customerId}/unread")
    @Operation(summary = "Get unread notifications")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable UUID customerId) {
        List<Notification> unread = notificationService.getUnreadNotifications(customerId);
        return ResponseEntity.ok(unread.stream().map(this::toResponse).toList());
    }

    @GetMapping("/customer/{customerId}/unread/count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID customerId) {
        long count = notificationService.getUnreadCount(customerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(toResponse(notification));
    }

    @PutMapping("/customer/{customerId}/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID customerId) {
        notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok().build();
    }

    // Preference management
    @GetMapping("/preferences/{customerId}")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<List<NotificationPreference>> getPreferences(@PathVariable UUID customerId) {
        return ResponseEntity.ok(notificationService.getPreferences(customerId));
    }

    @PutMapping("/preferences/{customerId}")
    @Operation(summary = "Update notification preference")
    public ResponseEntity<NotificationPreference> updatePreference(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        NotificationPreference pref = notificationService.updatePreference(
                customerId, request.type(), request.emailEnabled(),
                request.smsEnabled(), request.pushEnabled(), request.inAppEnabled()
        );
        return ResponseEntity.ok(pref);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getCustomerId(), n.getChannel(), n.getType(),
                n.getSubject(), n.getContent(), n.getStatus(), n.getPriority(),
                n.getSentAt(), n.getReadAt(), n.getCreatedAt()
        );
    }

    public record SendNotificationRequest(
            UUID customerId,
            UUID userId,
            @NotNull NotificationChannel channel,
            @NotNull NotificationType type,
            @NotBlank String recipient,
            String subject,
            @NotBlank String content,
            NotificationPriority priority,
            String templateName
    ) {}

    public record UpdatePreferenceRequest(
            @NotNull NotificationType type,
            boolean emailEnabled,
            boolean smsEnabled,
            boolean pushEnabled,
            boolean inAppEnabled
    ) {}

    public record NotificationResponse(
            UUID id,
            UUID customerId,
            NotificationChannel channel,
            NotificationType type,
            String subject,
            String content,
            NotificationStatus status,
            NotificationPriority priority,
            java.time.Instant sentAt,
            java.time.Instant readAt,
            java.time.Instant createdAt
    ) {}
}
