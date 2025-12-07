package com.banking.card.repository;

import com.banking.card.domain.CardNotification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardNotificationRepository extends JpaRepository<CardNotification, UUID> {
    Page<CardNotification> findByCardIdOrderByCreatedAtDesc(UUID cardId, Pageable pageable);
}

