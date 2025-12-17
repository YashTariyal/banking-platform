package com.banking.notification.repository;

import com.banking.notification.domain.Notification.NotificationType;
import com.banking.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByCustomerId(UUID customerId);

    Optional<NotificationPreference> findByCustomerIdAndNotificationType(UUID customerId, NotificationType type);
}
