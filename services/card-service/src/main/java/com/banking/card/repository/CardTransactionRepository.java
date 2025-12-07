package com.banking.card.repository;

import com.banking.card.domain.CardTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID> {
    Page<CardTransaction> findByCardIdOrderByTransactionDateDesc(UUID cardId, Pageable pageable);
}

