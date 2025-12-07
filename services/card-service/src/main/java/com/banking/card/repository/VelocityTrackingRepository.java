package com.banking.card.repository;

import com.banking.card.domain.VelocityTracking;
import com.banking.card.domain.VelocityWindow;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VelocityTrackingRepository extends JpaRepository<VelocityTracking, UUID> {
    Optional<VelocityTracking> findByCardIdAndWindowTypeAndWindowStart(
            UUID cardId, VelocityWindow windowType, Instant windowStart);
    List<VelocityTracking> findByCardIdAndWindowTypeAndWindowStartAfter(
            UUID cardId, VelocityWindow windowType, Instant windowStart);
}

