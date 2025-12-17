package com.banking.notification.service;

import com.banking.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final boolean enabled;

    public PushNotificationService(@Value("${notification.push.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Async
    public void sendPushNotification(String deviceToken, String title, String body) {
        if (!enabled) {
            log.info("Push notifications disabled, would send to device: {} - {}", title, body);
            return;
        }

        // Implementation would integrate with Firebase Cloud Messaging, Apple APNS, etc.
        log.info("Sending push notification: {} - {}", title, body);
        sendViaFcm(deviceToken, title, body);
    }

    public void sendNotificationPush(Notification notification, String deviceToken) {
        sendPushNotification(deviceToken, notification.getSubject(), notification.getContent());
    }

    private void sendViaFcm(String deviceToken, String title, String body) {
        // Firebase Cloud Messaging integration placeholder
        log.info("FCM push sent to device token: {}...", deviceToken.substring(0, Math.min(10, deviceToken.length())));
    }
}
