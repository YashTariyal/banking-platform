package com.banking.customer.repository;

import com.banking.customer.domain.ContactInfo;
import com.banking.customer.domain.ContactType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfo, UUID> {

    List<ContactInfo> findByCustomerId(UUID customerId);

    Optional<ContactInfo> findByCustomerIdAndIsPrimaryTrue(UUID customerId);

    Optional<ContactInfo> findByCustomerIdAndContactType(UUID customerId, ContactType contactType);

    @Query("SELECT ci FROM ContactInfo ci WHERE ci.customerId = :customerId AND ci.isPrimary = true")
    Optional<ContactInfo> findPrimaryContact(@Param("customerId") UUID customerId);

    @Query("SELECT ci FROM ContactInfo ci WHERE ci.email = :email")
    Optional<ContactInfo> findByEmail(@Param("email") String email);

    void deleteByCustomerId(UUID customerId);
}

