package com.banking.identity.repository;

import com.banking.identity.domain.MFASettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MFASettingsRepository extends JpaRepository<MFASettings, UUID> {

    Optional<MFASettings> findByUserId(UUID userId);
}

