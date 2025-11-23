package com.banking.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import com.banking.account.domain.AccountTransactionLog;
import com.banking.account.domain.AccountTransactionType;
import com.banking.account.domain.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountTransactionLogRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("accounts_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionLogRepository transactionLogRepository;

    @Test
    void findByAccountIdOrderByCreatedAtDescReturnsTransactionsInDescendingOrder() {
        Account account = saveAccount(UUID.randomUUID(), "ACC-111111111111");
        
        UUID ref1 = UUID.randomUUID();
        UUID ref2 = UUID.randomUUID();
        UUID ref3 = UUID.randomUUID();
        
        saveTransactionLog(account.getId(), ref1, AccountTransactionType.CREDIT, new BigDecimal("100.00"), Instant.now().minusSeconds(300));
        saveTransactionLog(account.getId(), ref2, AccountTransactionType.DEBIT, new BigDecimal("50.00"), Instant.now().minusSeconds(200));
        saveTransactionLog(account.getId(), ref3, AccountTransactionType.CREDIT, new BigDecimal("25.00"), Instant.now().minusSeconds(100));

        List<AccountTransactionLog> transactions = transactionLogRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());

        assertThat(transactions).hasSize(3);
        assertThat(transactions.get(0).getReferenceId()).isEqualTo(ref3); // Most recent first
        assertThat(transactions.get(1).getReferenceId()).isEqualTo(ref2);
        assertThat(transactions.get(2).getReferenceId()).isEqualTo(ref1);
    }

    @Test
    void findByAccountIdOrderByCreatedAtDescWithPagination() {
        Account account = saveAccount(UUID.randomUUID(), "ACC-222222222222");
        
        // Create 5 transactions
        for (int i = 0; i < 5; i++) {
            saveTransactionLog(account.getId(), UUID.randomUUID(), AccountTransactionType.CREDIT, 
                    new BigDecimal("10.00"), Instant.now().minusSeconds(100 - i));
        }

        Page<AccountTransactionLog> page = transactionLogRepository.findByAccountIdOrderByCreatedAtDesc(
                account.getId(), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByAccountIdAndReferenceIdReturnsSpecificTransaction() {
        Account account = saveAccount(UUID.randomUUID(), "ACC-333333333333");
        UUID referenceId = UUID.randomUUID();
        
        saveTransactionLog(account.getId(), referenceId, 
                AccountTransactionType.CREDIT, new BigDecimal("200.00"), Instant.now());

        var found = transactionLogRepository.findByAccountIdAndReferenceId(account.getId(), referenceId);

        assertThat(found).isPresent();
        assertThat(found.get().getReferenceId()).isEqualTo(referenceId);
        assertThat(found.get().getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void findByAccountIdOrderByCreatedAtDescReturnsEmptyForNonExistentAccount() {
        List<AccountTransactionLog> transactions = transactionLogRepository.findByAccountIdOrderByCreatedAtDesc(UUID.randomUUID());
        assertThat(transactions).isEmpty();
    }

    private Account saveAccount(UUID customerId, String accountNumber) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setAccountNumber(accountNumber);
        account.setCustomerId(customerId);
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("USD");
        account.setBalance(new BigDecimal("100.00"));
        account.setOpenedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return accountRepository.save(account);
    }

    private AccountTransactionLog saveTransactionLog(UUID accountId, UUID referenceId, 
                                                     AccountTransactionType type, BigDecimal amount, Instant createdAt) {
        AccountTransactionLog log = new AccountTransactionLog();
        log.setAccountId(accountId);
        log.setReferenceId(referenceId);
        log.setType(type);
        log.setAmount(amount);
        log.setResultingBalance(new BigDecimal("100.00"));
        log.setCreatedAt(createdAt);
        return transactionLogRepository.save(log);
    }
}

