package com.banking.card.repository;

import com.banking.card.domain.CardTransfer;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardTransferRepository extends JpaRepository<CardTransfer, UUID> {
    Page<CardTransfer> findByFromCardIdOrderByTransferDateDesc(UUID fromCardId, Pageable pageable);
    Page<CardTransfer> findByToCardIdOrderByTransferDateDesc(UUID toCardId, Pageable pageable);
}

