package com.banking.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.transaction.domain.Transaction;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.domain.TransactionType;
import com.banking.transaction.messaging.TransactionEventPublisher;
import com.banking.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventPublisher eventPublisher;

    private Clock fixedClock;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        transactionService = new TransactionService(transactionRepository, eventPublisher, fixedClock);
    }

    @Test
    void createTransaction_generatesReferenceIdAndSaves() {
        UUID customerId = UUID.randomUUID();
        String referenceId = "REF-123";

        when(transactionRepository.findByReferenceId(referenceId))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(UUID.randomUUID());
                    return transaction;
                });

        Transaction result = transactionService.createTransaction(
                referenceId,
                TransactionType.DEPOSIT,
                customerId,
                null,
                null,
                new BigDecimal("100"),
                "USD",
                "Test deposit"
        );

        assertThat(result).isNotNull();
        assertThat(result.getReferenceId()).isEqualTo(referenceId);
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(result.getCustomerId()).isEqualTo(customerId);

        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publishTransactionInitiated(any(Transaction.class));
    }

    @Test
    void createTransaction_duplicateReferenceId_throwsException() {
        String referenceId = "REF-123";

        when(transactionRepository.findByReferenceId(referenceId))
                .thenReturn(Optional.of(new Transaction()));

        assertThatThrownBy(() -> transactionService.createTransaction(
                referenceId,
                TransactionType.DEPOSIT,
                UUID.randomUUID(),
                null,
                null,
                new BigDecimal("100"),
                "USD",
                "Test"
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void processTransaction_updatesStatus() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.processTransaction(transactionId);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
        verify(eventPublisher).publishTransactionProcessing(any(Transaction.class));
    }

    @Test
    void completeTransaction_updatesStatusAndSetsCompletedAt() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PROCESSING);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.completeTransaction(transactionId);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishTransactionCompleted(any(Transaction.class));
    }

    @Test
    void failTransaction_updatesStatus() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PROCESSING);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.failTransaction(transactionId, "Insufficient funds");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Insufficient funds");
        verify(eventPublisher).publishTransactionFailed(any(Transaction.class));
    }

    @Test
    void cancelTransaction_onlyPending_allowsCancellation() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.cancelTransaction(transactionId);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(eventPublisher).publishTransactionCancelled(any(Transaction.class));
    }

    @Test
    void reverseTransaction_onlyCompleted_allowsReversal() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.COMPLETED);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.reverseTransaction(transactionId, "Customer request");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        assertThat(result.getFailureReason()).isEqualTo("Customer request");
        verify(eventPublisher).publishTransactionReversed(any(Transaction.class));
    }
}

