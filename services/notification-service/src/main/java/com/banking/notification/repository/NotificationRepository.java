package com.banking.notification.repository;

import com.banking.notification.domain.Notification;
import com.banking.notification.domain.Notification.NotificationChannel;
import com.banking.notification.domain.Notification.NotificationStatus;
import com.banking.notification.domain.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < :maxRetries")
    List<Notification> findPendingForRetry(@Param("status") NotificationStatus status, @Param("maxRetries") int maxRetries);

    @Query("SELECT n FROM Notification n WHERE n.customerId = :customerId AND n.status = 'SENT' AND n.readAt IS NULL")
    List<Notification> findUnreadByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.customerId = :customerId AND n.status = 'SENT' AND n.readAt IS NULL")
    long countUnreadByCustomerId(@Param("customerId") UUID customerId);

    Page<Notification> findByCustomerIdAndType(UUID customerId, NotificationType type, Pageable pageable);

    Page<Notification> findByCustomerIdAndChannel(UUID customerId, NotificationChannel channel, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :since AND n.type = :type")
    List<Notification> findByTypeSince(@Param("type") NotificationType type, @Param("since") Instant since);
}
