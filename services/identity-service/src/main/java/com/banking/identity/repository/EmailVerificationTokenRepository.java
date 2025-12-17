package com.banking.identity.repository;

import com.banking.identity.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.token = :token AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<EmailVerificationToken> findValidToken(@Param("token") String token);

    Optional<EmailVerificationToken> findByToken(String token);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.userId = :userId AND t.usedAt IS NULL ORDER BY t.createdAt DESC")
    Optional<EmailVerificationToken> findLatestByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
