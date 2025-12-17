package com.banking.notification.service;

import com.banking.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final boolean enabled;

    public EmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${notification.email.from:noreply@banking-platform.com}") String fromAddress,
            @Value("${notification.email.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!enabled) {
            log.info("Email disabled, would send to {}: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!enabled) {
            log.info("Email disabled, would send HTML to {}: {}", to, subject);
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendNotificationEmail(Notification notification) {
        if (!enabled) {
            log.info("Email disabled, would send notification {}", notification.getId());
            return;
        }

        try {
            if (notification.getTemplateName() != null) {
                // Use template if available
                Map<String, Object> variables = Map.of(
                    "subject", notification.getSubject() != null ? notification.getSubject() : "",
                    "content", notification.getContent(),
                    "type", notification.getType().name()
                );
                sendHtmlEmail(notification.getRecipient(), notification.getSubject(), notification.getTemplateName(), variables);
            } else {
                sendSimpleEmail(notification.getRecipient(), notification.getSubject(), notification.getContent());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification email", e);
        }
    }
}
