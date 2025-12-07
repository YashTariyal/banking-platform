package com.banking.card.repository;

import com.banking.card.domain.GeographicRestriction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeographicRestrictionRepository extends JpaRepository<GeographicRestriction, UUID> {
    List<GeographicRestriction> findByCardId(UUID cardId);
    Optional<GeographicRestriction> findByCardIdAndCountryCode(UUID cardId, String countryCode);
    void deleteByCardId(UUID cardId);
}

