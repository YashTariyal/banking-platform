package com.banking.notification.messaging;

import com.banking.notification.domain.Notification.NotificationChannel;
import com.banking.notification.domain.Notification.NotificationPriority;
import com.banking.notification.domain.Notification.NotificationType;
import com.banking.notification.service.NotificationService;
import com.banking.notification.service.NotificationService.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final NotificationService notificationService;
    private final BigDecimal lowBalanceThreshold;
    private final BigDecimal suspiciousActivityThreshold;

    public EventConsumer(
            NotificationService notificationService,
            @Value("${notification.alerts.low-balance-threshold:100.00}") BigDecimal lowBalanceThreshold,
            @Value("${notification.alerts.suspicious-activity-threshold:10000.00}") BigDecimal suspiciousActivityThreshold) {
        this.notificationService = notificationService;
        this.lowBalanceThreshold = lowBalanceThreshold;
        this.suspiciousActivityThreshold = suspiciousActivityThreshold;
    }

    @KafkaListener(topics = "identity-events", groupId = "notification-service")
    public void handleIdentityEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received identity event: {}", eventType);

        switch (eventType) {
            case "PASSWORD_RESET_REQUESTED" -> handlePasswordResetRequested(event);
            case "EMAIL_VERIFICATION_REQUESTED" -> handleEmailVerificationRequested(event);
            case "USER_LOCKED" -> handleUserLocked(event);
            case "USER_LOGGED_IN" -> handleUserLoggedIn(event);
            default -> log.debug("Ignoring identity event: {}", eventType);
        }
    }

    @KafkaListener(topics = "transaction-events", groupId = "notification-service")
    public void handleTransactionEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received transaction event: {}", eventType);

        switch (eventType) {
            case "TRANSACTION_COMPLETED" -> handleTransactionCompleted(event);
            case "PAYMENT_RECEIVED" -> handlePaymentReceived(event);
            case "LOW_BALANCE" -> handleLowBalance(event);
            case "SUSPICIOUS_ACTIVITY" -> handleSuspiciousActivity(event);
            default -> log.debug("Ignoring transaction event: {}", eventType);
        }
    }

    @KafkaListener(topics = "account-events", groupId = "notification-service")
    public void handleAccountEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received account event: {}", eventType);

        if ("BALANCE_UPDATED".equals(eventType)) {
            checkLowBalance(event);
        }
    }

    private void handlePasswordResetRequested(Map<String, Object> event) {
        String email = getMetadataValue(event, "email");
        String token = getMetadataValue(event, "token");
        UUID userId = parseUuid(event.get("userId"));

        if (email != null && token != null) {
            String resetLink = "https://banking-platform.com/reset-password?token=" + token;
            String content = "Click the link below to reset your password:\n\n" + resetLink +
                    "\n\nThis link will expire in 1 hour.";

            notificationService.createAndSendNotification(new NotificationRequest(
                    null, userId, NotificationChannel.EMAIL, NotificationType.PASSWORD_RESET,
                    email, "Password Reset Request", content, NotificationPriority.HIGH, null, null
            ));
        }
    }

    private void handleEmailVerificationRequested(Map<String, Object> event) {
        String email = getMetadataValue(event, "email");
        String token = getMetadataValue(event, "token");
        UUID userId = parseUuid(event.get("userId"));

        if (email != null && token != null) {
            String verifyLink = "https://banking-platform.com/verify-email?token=" + token;
            String content = "Welcome to Banking Platform!\n\nPlease verify your email by clicking the link below:\n\n" +
                    verifyLink + "\n\nThis link will expire in 24 hours.";

            notificationService.createAndSendNotification(new NotificationRequest(
                    null, userId, NotificationChannel.EMAIL, NotificationType.EMAIL_VERIFICATION,
                    email, "Verify Your Email Address", content, NotificationPriority.HIGH, null, null
            ));
        }
    }

    private void handleUserLocked(Map<String, Object> event) {
        UUID userId = parseUuid(event.get("userId"));
        String username = (String) event.get("username");
        
        // Would need to fetch email from user service
        log.info("User {} ({}) has been locked due to too many failed login attempts", username, userId);
    }

    private void handleUserLoggedIn(Map<String, Object> event) {
        UUID userId = parseUuid(event.get("userId"));
        // Could send login alert for suspicious login locations
        log.debug("User {} logged in", userId);
    }

    private void handleTransactionCompleted(Map<String, Object> event) {
        UUID customerId = parseUuid(event.get("customerId"));
        String amount = String.valueOf(event.get("amount"));
        String type = (String) event.get("transactionType");
        
        // Check for suspicious activity
        BigDecimal txAmount = new BigDecimal(amount);
        if (txAmount.compareTo(suspiciousActivityThreshold) > 0) {
            handleSuspiciousActivity(event);
        }
    }

    private void handlePaymentReceived(Map<String, Object> event) {
        UUID customerId = parseUuid(event.get("customerId"));
        String email = (String) event.get("email");
        String amount = String.valueOf(event.get("amount"));
        String fromAccount = (String) event.get("fromAccount");

        if (email != null) {
            String content = String.format("You have received a payment of $%s from account %s.", amount, fromAccount);

            notificationService.createAndSendNotification(new NotificationRequest(
                    customerId, null, NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED,
                    email, "Payment Received", content, NotificationPriority.NORMAL, null, null
            ));
        }
    }

    private void handleLowBalance(Map<String, Object> event) {
        UUID customerId = parseUuid(event.get("customerId"));
        String email = (String) event.get("email");
        String balance = String.valueOf(event.get("balance"));
        String accountNumber = (String) event.get("accountNumber");

        if (email != null) {
            String content = String.format(
                    "Alert: Your account %s balance is now $%s, which is below the threshold of $%s.",
                    accountNumber, balance, lowBalanceThreshold
            );

            notificationService.createAndSendNotification(new NotificationRequest(
                    customerId, null, NotificationChannel.EMAIL, NotificationType.LOW_BALANCE_ALERT,
                    email, "Low Balance Alert", content, NotificationPriority.HIGH, null, null
            ));
        }
    }

    private void handleSuspiciousActivity(Map<String, Object> event) {
        UUID customerId = parseUuid(event.get("customerId"));
        String email = (String) event.get("email");
        String amount = String.valueOf(event.get("amount"));
        String details = (String) event.get("details");

        if (email != null) {
            String content = String.format(
                    "SECURITY ALERT: We detected unusual activity on your account.\n\n" +
                    "Amount: $%s\nDetails: %s\n\n" +
                    "If you did not authorize this transaction, please contact us immediately.",
                    amount, details != null ? details : "Large transaction detected"
            );

            notificationService.createAndSendNotification(new NotificationRequest(
                    customerId, null, NotificationChannel.EMAIL, NotificationType.SUSPICIOUS_ACTIVITY,
                    email, "Security Alert: Unusual Activity Detected", content, NotificationPriority.URGENT, null, null
            ));
        }
    }

    private void checkLowBalance(Map<String, Object> event) {
        String balanceStr = String.valueOf(event.get("balance"));
        if (balanceStr != null && !balanceStr.equals("null")) {
            BigDecimal balance = new BigDecimal(balanceStr);
            if (balance.compareTo(lowBalanceThreshold) < 0) {
                handleLowBalance(event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getMetadataValue(Map<String, Object> event, String key) {
        Object metadata = event.get("metadata");
        if (metadata instanceof Map) {
            return (String) ((Map<String, Object>) metadata).get(key);
        }
        return (String) event.get(key);
    }

    private UUID parseUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return (UUID) value;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
