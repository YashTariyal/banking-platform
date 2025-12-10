package com.banking.customer.repository;

import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    Page<Customer> findByCustomerType(CustomerType customerType, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL")
    Page<Customer> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.nationalId = :nationalId AND c.nationalIdType = :nationalIdType AND c.deletedAt IS NULL")
    Optional<Customer> findByNationalId(@Param("nationalId") String nationalId, @Param("nationalIdType") String nationalIdType);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status AND c.deletedAt IS NULL")
    long countByStatus(@Param("status") CustomerStatus status);

    boolean existsByCustomerNumber(String customerNumber);
}

