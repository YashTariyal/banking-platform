package com.banking.card.repository;

import com.banking.card.domain.AuthorizationRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationRequestRepository extends JpaRepository<AuthorizationRequest, UUID> {
    Page<AuthorizationRequest> findByCardIdOrderByCheckedAtDesc(UUID cardId, Pageable pageable);
}

