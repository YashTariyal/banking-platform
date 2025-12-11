package com.banking.support.repository;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualOverrideRepository extends JpaRepository<ManualOverride, UUID> {

    Page<ManualOverride> findByStatus(OverrideStatus status, Pageable pageable);

    Page<ManualOverride> findByOverrideType(OverrideType overrideType, Pageable pageable);

    Page<ManualOverride> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ManualOverride> findByAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT o FROM ManualOverride o WHERE o.status = :status AND o.expiresAt > :now")
    List<ManualOverride> findActiveOverrides(@Param("status") OverrideStatus status, @Param("now") java.time.Instant now);

    @Query("SELECT o FROM ManualOverride o WHERE o.customerId = :customerId AND o.overrideType = :overrideType AND o.status = :status")
    List<ManualOverride> findByCustomerAndTypeAndStatus(
            @Param("customerId") UUID customerId,
            @Param("overrideType") OverrideType overrideType,
            @Param("status") OverrideStatus status
    );
}

