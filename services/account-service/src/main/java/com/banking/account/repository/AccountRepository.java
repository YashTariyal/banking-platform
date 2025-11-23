package com.banking.account.repository;

import com.banking.account.domain.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    Page<Account> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.deletedAt IS NULL")
    Page<Account> findAll(Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Account> findById(UUID id);

    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.deletedAt IS NULL")
    Optional<Account> findByAccountNumber(String accountNumber);
}

