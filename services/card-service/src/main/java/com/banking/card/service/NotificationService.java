package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardNotification;
import com.banking.card.domain.NotificationStatus;
import com.banking.card.repository.CardNotificationRepository;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.dto.CreateNotificationRequest;
import com.banking.card.web.dto.NotificationResponse;
import com.banking.card.web.dto.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final CardRepository cardRepository;
    private final CardNotificationRepository notificationRepository;

    public NotificationService(CardRepository cardRepository, CardNotificationRepository notificationRepository) {
        this.cardRepository = cardRepository;
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse createNotification(UUID cardId, CreateNotificationRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        CardNotification notification = new CardNotification();
        notification.setId(UUID.randomUUID());
        notification.setCard(card);
        notification.setNotificationType(request.notificationType());
        notification.setChannel(request.channel());
        notification.setSubject(request.subject());
        notification.setMessage(request.message());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(Instant.now());

        // In a real system, this would send the notification via Kafka or external service
        // For now, we'll mark it as sent immediately
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());

        CardNotification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID cardId, int page, int size) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }

        Page<CardNotification> notifications = notificationRepository.findByCardIdOrderByCreatedAtDesc(
                cardId, PageRequest.of(page, size));
        return PageResponse.from(notifications.map(this::toResponse));
    }

    private NotificationResponse toResponse(CardNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCard().getId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}

