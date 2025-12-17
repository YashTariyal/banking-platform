package com.banking.identity.repository;

import com.banking.identity.domain.ApiKey;
import com.banking.identity.domain.ApiKey.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT k FROM ApiKey k WHERE k.keyPrefix = :prefix AND k.status = 'ACTIVE'")
    List<ApiKey> findActiveByPrefix(@Param("prefix") String prefix);

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByServiceName(String serviceName);

    List<ApiKey> findByStatus(ApiKeyStatus status);

    @Query("SELECT k FROM ApiKey k WHERE k.createdBy = :userId")
    List<ApiKey> findByCreatedBy(@Param("userId") UUID userId);
}
