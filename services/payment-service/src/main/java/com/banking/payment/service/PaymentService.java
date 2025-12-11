package com.banking.payment.service;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import com.banking.payment.messaging.PaymentEvent;
import com.banking.payment.messaging.PaymentEventPublisher;
import com.banking.payment.repository.PaymentRepository;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventPublisher eventPublisher,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Payment initiatePayment(
            String referenceId,
            PaymentRail rail,
            PaymentDirection direction,
            UUID fromAccountId,
            UUID toAccountId,
            String toExternalAccount,
            String toExternalRouting,
            String toExternalBankName,
            BigDecimal amount,
            String currency,
            String description
    ) {
        // Check for duplicate reference ID
        if (paymentRepository.findByReferenceId(referenceId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment with reference ID already exists: " + referenceId
            );
        }

        Payment payment = new Payment();
        payment.setReferenceId(referenceId);
        payment.setRail(rail);
        payment.setDirection(direction);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountId(toAccountId);
        payment.setToExternalAccount(toExternalAccount);
        payment.setToExternalRouting(toExternalRouting);
        payment.setToExternalBankName(toExternalBankName);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setDescription(description);
        payment.setInitiatedAt(Instant.now(clock));

        Payment saved = paymentRepository.save(payment);

        // Publish payment initiated event
        eventPublisher.publishPaymentInitiated(saved);

        return saved;
    }

    @Transactional
    public Payment processPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not in PENDING status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        Payment updated = paymentRepository.save(payment);

        // Publish payment processing event
        eventPublisher.publishPaymentProcessing(updated);

        // Simulate external rail processing
        // In a real system, this would call external payment rail APIs
        simulateRailProcessing(updated);

        return updated;
    }

    @Transactional
    public Payment completePayment(UUID paymentId, String externalReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));

        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not in PROCESSING status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setExternalReference(externalReference);
        payment.setCompletedAt(Instant.now(clock));
        Payment updated = paymentRepository.save(payment);

        // Publish payment completed event
        eventPublisher.publishPaymentCompleted(updated);

        return updated;
    }

    @Transactional
    public Payment failPayment(UUID paymentId, String failureReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot fail payment in status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        payment.setCompletedAt(Instant.now(clock));
        Payment updated = paymentRepository.save(payment);

        // Publish payment failed event
        eventPublisher.publishPaymentFailed(updated);

        return updated;
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel payment in status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment updated = paymentRepository.save(payment);

        // Publish payment cancelled event
        eventPublisher.publishPaymentCancelled(updated);

        return updated;
    }

    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));
    }

    public Payment getPaymentByReferenceId(String referenceId) {
        return paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found with reference ID: " + referenceId
                ));
    }

    public Page<Payment> getPaymentsByAccount(UUID accountId, Pageable pageable) {
        return paymentRepository.findByAccountId(accountId, pageable);
    }

    public Page<Payment> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable);
    }

    private void simulateRailProcessing(Payment payment) {
        // Simulate async processing - in real system, this would be handled by async workers
        // For now, we'll just mark it as processing
        // External rail integration would happen here
    }
}

