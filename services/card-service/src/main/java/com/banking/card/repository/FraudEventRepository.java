package com.banking.card.repository;

import com.banking.card.domain.FraudEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudEventRepository extends JpaRepository<FraudEvent, UUID> {
    Page<FraudEvent> findByCardIdOrderByDetectedAtDesc(UUID cardId, Pageable pageable);
    List<FraudEvent> findByCardIdAndResolvedFalse(UUID cardId);
    @Query("SELECT COUNT(f) FROM FraudEvent f WHERE f.card.id = :cardId AND f.resolved = false")
    Long countUnresolvedByCardId(UUID cardId);
}

