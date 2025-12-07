package com.banking.card.repository;

import com.banking.card.domain.MerchantRestriction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRestrictionRepository extends JpaRepository<MerchantRestriction, UUID> {
    List<MerchantRestriction> findByCardId(UUID cardId);
    Optional<MerchantRestriction> findByCardIdAndMerchantCategoryCode(UUID cardId, String merchantCategoryCode);
    void deleteByCardId(UUID cardId);
}

