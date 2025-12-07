package com.banking.card.repository;

import com.banking.card.domain.CardAnalytics;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAnalyticsRepository extends JpaRepository<CardAnalytics, UUID> {
    Optional<CardAnalytics> findByCardId(UUID cardId);
}

