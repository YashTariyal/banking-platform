package com.banking.transaction.service;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.messaging.TransactionEventPublisher;
import com.banking.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionEventPublisher eventPublisher,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Transaction createTransaction(
            String referenceId,
            TransactionType transactionType,
            UUID customerId,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            String currency,
            String description
    ) {
        // Check idempotency
        if (referenceId != null) {
            transactionRepository.findByReferenceId(referenceId)
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Transaction with reference ID already exists: " + referenceId
                        );
                    });
        }

        Transaction transaction = new Transaction();
        transaction.setReferenceId(referenceId != null ? referenceId : generateReferenceId());
        transaction.setTransactionType(transactionType);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCustomerId(customerId);
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setDescription(description);
        transaction.setInitiatedAt(Instant.now(clock));

        Transaction saved = transactionRepository.save(transaction);
        eventPublisher.publishTransactionInitiated(saved);

        return saved;
    }

    @Transactional
    public Transaction processTransaction(UUID transactionId) {
        Transaction transaction = getTransaction(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transaction is not in PENDING status: " + transaction.getStatus()
            );
        }

        transaction.setStatus(TransactionStatus.PROCESSING);
        Transaction updated = transactionRepository.save(transaction);

        eventPublisher.publishTransactionProcessing(updated);

        return updated;
    }

    @Transactional
    public Transaction completeTransaction(UUID transactionId) {
        Transaction transaction = getTransaction(transactionId);

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transaction is not in PROCESSING status: " + transaction.getStatus()
            );
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now(clock));
        Transaction updated = transactionRepository.save(transaction);

        eventPublisher.publishTransactionCompleted(updated);

        return updated;
    }

    @Transactional
    public Transaction failTransaction(UUID transactionId, String failureReason) {
        Transaction transaction = getTransaction(transactionId);

        if (transaction.getStatus() == TransactionStatus.COMPLETED || transaction.getStatus() == TransactionStatus.REVERSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot fail transaction in status: " + transaction.getStatus()
            );
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(failureReason);
        Transaction updated = transactionRepository.save(transaction);

        eventPublisher.publishTransactionFailed(updated);

        return updated;
    }

    @Transactional
    public Transaction cancelTransaction(UUID transactionId) {
        Transaction transaction = getTransaction(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PENDING transactions can be cancelled"
            );
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        Transaction updated = transactionRepository.save(transaction);

        eventPublisher.publishTransactionCancelled(updated);

        return updated;
    }

    @Transactional
    public Transaction reverseTransaction(UUID transactionId, String reason) {
        Transaction transaction = getTransaction(transactionId);

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COMPLETED transactions can be reversed"
            );
        }

        transaction.setStatus(TransactionStatus.REVERSED);
        transaction.setFailureReason(reason);
        Transaction updated = transactionRepository.save(transaction);

        eventPublisher.publishTransactionReversed(updated);

        return updated;
    }

    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId
                ));
    }

    public Transaction getTransactionByReferenceId(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + referenceId
                ));
    }

    public Page<Transaction> getTransactionsByStatus(TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStatus(status, pageable);
    }

    public Page<Transaction> getTransactionsByType(TransactionType transactionType, Pageable pageable) {
        return transactionRepository.findByTransactionType(transactionType, pageable);
    }

    public Page<Transaction> getTransactionsByCustomer(UUID customerId, Pageable pageable) {
        return transactionRepository.findByCustomerId(customerId, pageable);
    }

    public Page<Transaction> getTransactionsByAccount(UUID accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    private String generateReferenceId() {
        return "TXN-" + Instant.now(clock).toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

