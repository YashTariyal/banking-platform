package com.banking.notification.service;

import com.banking.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final boolean enabled;
    private final String provider;

    public SmsService(
            @Value("${notification.sms.enabled:false}") boolean enabled,
            @Value("${notification.sms.provider:twilio}") String provider) {
        this.enabled = enabled;
        this.provider = provider;
    }

    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!enabled) {
            log.info("SMS disabled, would send to {}: {}", phoneNumber, message);
            return;
        }

        // Implementation would integrate with Twilio, AWS SNS, etc.
        log.info("Sending SMS via {} to {}: {}", provider, phoneNumber, message);
        
        switch (provider.toLowerCase()) {
            case "twilio" -> sendViaTwilio(phoneNumber, message);
            case "aws" -> sendViaAwsSns(phoneNumber, message);
            default -> log.warn("Unknown SMS provider: {}", provider);
        }
    }

    public void sendNotificationSms(Notification notification) {
        sendSms(notification.getRecipient(), notification.getContent());
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        // Twilio integration placeholder
        log.info("Twilio SMS sent to {}", phoneNumber);
    }

    private void sendViaAwsSns(String phoneNumber, String message) {
        // AWS SNS integration placeholder
        log.info("AWS SNS SMS sent to {}", phoneNumber);
    }
}
