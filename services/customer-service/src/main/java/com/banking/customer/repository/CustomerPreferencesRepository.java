package com.banking.customer.repository;

import com.banking.customer.domain.CustomerPreferences;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPreferencesRepository extends JpaRepository<CustomerPreferences, UUID> {

    Optional<CustomerPreferences> findByCustomerId(UUID customerId);

    void deleteByCustomerId(UUID customerId);
}

