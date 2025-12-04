package com.banking.card.repository;

import com.banking.card.domain.Card;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Page<Card> findByCustomerId(UUID customerId, Pageable pageable);

    List<Card> findByCustomerId(UUID customerId);
}


