package com.banking.identity.repository;

import com.banking.identity.domain.Session;
import com.banking.identity.domain.SessionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshToken(String refreshToken);

    List<Session> findByUserId(UUID userId);

    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.status = :status")
    List<Session> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") SessionStatus status);

    @Modifying
    @Query("UPDATE Session s SET s.status = :status WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    void revokeAllActiveSessions(@Param("userId") UUID userId, @Param("status") SessionStatus status);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredSessions();
}

