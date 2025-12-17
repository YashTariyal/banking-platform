package com.banking.notification.service;

import com.banking.notification.domain.Notification;
import com.banking.notification.domain.Notification.*;
import com.banking.notification.domain.NotificationPreference;
import com.banking.notification.repository.NotificationPreferenceRepository;
import com.banking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            EmailService emailService,
            SmsService smsService,
            PushNotificationService pushService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.pushService = pushService;
    }

    @Transactional
    public Notification createAndSendNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setCustomerId(request.customerId());
        notification.setUserId(request.userId());
        notification.setChannel(request.channel());
        notification.setType(request.type());
        notification.setRecipient(request.recipient());
        notification.setSubject(request.subject());
        notification.setContent(request.content());
        notification.setPriority(request.priority() != null ? request.priority() : NotificationPriority.NORMAL);
        notification.setTemplateName(request.templateName());
        notification.setMetadata(request.metadata());
        notification.setStatus(NotificationStatus.PENDING);

        notification = notificationRepository.save(notification);
        sendNotification(notification);
        return notification;
    }

    @Transactional
    public void sendNotification(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case EMAIL -> emailService.sendNotificationEmail(notification);
                case SMS -> smsService.sendNotificationSms(notification);
                case PUSH -> pushService.sendNotificationPush(notification, notification.getRecipient());
                case IN_APP -> log.info("In-app notification stored: {}", notification.getId());
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Notification {} sent via {}", notification.getId(), notification.getChannel());
        } catch (Exception e) {
            log.error("Failed to send notification {}", notification.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
        }
        notificationRepository.save(notification);
    }

    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository.findPendingForRetry(NotificationStatus.FAILED, MAX_RETRIES);
        for (Notification notification : failed) {
            log.info("Retrying notification {} (attempt {})", notification.getId(), notification.getRetryCount() + 1);
            sendNotification(notification);
        }
    }

    public Page<Notification> getCustomerNotifications(UUID customerId, Pageable pageable) {
        return notificationRepository.findByCustomerId(customerId, pageable);
    }

    public List<Notification> getUnreadNotifications(UUID customerId) {
        return notificationRepository.findUnreadByCustomerId(customerId);
    }

    public long getUnreadCount(UUID customerId) {
        return notificationRepository.countUnreadByCustomerId(customerId);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setReadAt(Instant.now());
        notification.setStatus(NotificationStatus.READ);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID customerId) {
        List<Notification> unread = notificationRepository.findUnreadByCustomerId(customerId);
        Instant now = Instant.now();
        for (Notification notification : unread) {
            notification.setReadAt(now);
            notification.setStatus(NotificationStatus.READ);
        }
        notificationRepository.saveAll(unread);
    }

    // Preference management
    public List<NotificationPreference> getPreferences(UUID customerId) {
        return preferenceRepository.findByCustomerId(customerId);
    }

    @Transactional
    public NotificationPreference updatePreference(UUID customerId, NotificationType type,
                                                   boolean email, boolean sms, boolean push, boolean inApp) {
        NotificationPreference pref = preferenceRepository.findByCustomerIdAndNotificationType(customerId, type)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setCustomerId(customerId);
                    newPref.setNotificationType(type);
                    return newPref;
                });
        pref.setEmailEnabled(email);
        pref.setSmsEnabled(sms);
        pref.setPushEnabled(push);
        pref.setInAppEnabled(inApp);
        return preferenceRepository.save(pref);
    }

    public record NotificationRequest(
            UUID customerId,
            UUID userId,
            NotificationChannel channel,
            NotificationType type,
            String recipient,
            String subject,
            String content,
            NotificationPriority priority,
            String templateName,
            String metadata
    ) {}
}
