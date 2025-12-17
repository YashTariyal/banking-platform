package com.banking.notification.domain;

import com.banking.notification.domain.Notification.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationCreation() {
        Notification notification = new Notification();
        notification.setCustomerId(UUID.randomUUID());
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setType(NotificationType.TRANSACTION_ALERT);
        notification.setRecipient("test@example.com");
        notification.setSubject("Test Subject");
        notification.setContent("Test Content");

        assertEquals(NotificationChannel.EMAIL, notification.getChannel());
        assertEquals(NotificationType.TRANSACTION_ALERT, notification.getType());
        assertEquals("test@example.com", notification.getRecipient());
    }

    @Test
    void testNotificationChannels() {
        assertEquals(4, NotificationChannel.values().length);
        assertNotNull(NotificationChannel.valueOf("EMAIL"));
        assertNotNull(NotificationChannel.valueOf("SMS"));
        assertNotNull(NotificationChannel.valueOf("PUSH"));
        assertNotNull(NotificationChannel.valueOf("IN_APP"));
    }

    @Test
    void testNotificationTypes() {
        assertTrue(NotificationType.values().length >= 10);
        assertNotNull(NotificationType.valueOf("TRANSACTION_ALERT"));
        assertNotNull(NotificationType.valueOf("LOW_BALANCE_ALERT"));
        assertNotNull(NotificationType.valueOf("SUSPICIOUS_ACTIVITY"));
        assertNotNull(NotificationType.valueOf("PAYMENT_RECEIVED"));
        assertNotNull(NotificationType.valueOf("PASSWORD_RESET"));
    }

    @Test
    void testNotificationStatus() {
        Notification notification = new Notification();
        notification.setStatus(NotificationStatus.PENDING);
        assertEquals(NotificationStatus.PENDING, notification.getStatus());

        notification.setStatus(NotificationStatus.SENT);
        assertEquals(NotificationStatus.SENT, notification.getStatus());
    }

    @Test
    void testNotificationPriority() {
        assertEquals(4, NotificationPriority.values().length);
        assertNotNull(NotificationPriority.valueOf("LOW"));
        assertNotNull(NotificationPriority.valueOf("NORMAL"));
        assertNotNull(NotificationPriority.valueOf("HIGH"));
        assertNotNull(NotificationPriority.valueOf("URGENT"));
    }
}
