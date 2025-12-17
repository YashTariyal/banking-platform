package com.banking.identity.repository;

import com.banking.identity.domain.PasswordReset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

    Optional<PasswordReset> findByToken(String token);

    @Query("SELECT pr FROM PasswordReset pr WHERE pr.userId = :userId AND pr.used = false AND pr.expiresAt > CURRENT_TIMESTAMP ORDER BY pr.createdAt DESC")
    Optional<PasswordReset> findLatestValidByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE PasswordReset pr SET pr.used = true, pr.usedAt = CURRENT_TIMESTAMP WHERE pr.userId = :userId AND pr.used = false")
    void invalidateAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE PasswordReset pr SET pr.used = true WHERE pr.userId = :userId AND pr.used = false")
    void invalidateExistingTokens(@Param("userId") UUID userId);

    @Query("SELECT pr FROM PasswordReset pr WHERE pr.token = :token AND pr.used = false AND pr.expiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordReset> findValidToken(@Param("token") String token);
}

