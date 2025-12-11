package com.banking.payment.service;

import com.banking.payment.domain.Payment;
import com.banking.payment.domain.PaymentDirection;
import com.banking.payment.domain.PaymentRail;
import com.banking.payment.domain.PaymentStatus;
import com.banking.payment.messaging.PaymentEventPublisher;
import com.banking.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private PaymentService paymentService;

    private Clock fixedClock;
    private UUID paymentId;
    private UUID fromAccountId;
    private UUID toAccountId;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        paymentId = UUID.randomUUID();
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
    }

    @Test
    void initiatePayment_CreatesNewPayment() {
        String referenceId = "REF-123";
        BigDecimal amount = new BigDecimal("100.00");

        when(clock.instant()).thenReturn(fixedClock.instant());
        when(paymentRepository.findByReferenceId(referenceId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.initiatePayment(
                referenceId,
                PaymentRail.ACH,
                PaymentDirection.OUTBOUND,
                fromAccountId,
                toAccountId,
                null,
                null,
                null,
                amount,
                "USD",
                "Test payment"
        );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getReferenceId()).isEqualTo(referenceId);
        assertThat(result.getRail()).isEqualTo(PaymentRail.ACH);
        assertThat(result.getDirection()).isEqualTo(PaymentDirection.OUTBOUND);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);

        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishPaymentInitiated(any(Payment.class));
    }

    @Test
    void initiatePayment_DuplicateReferenceId_ThrowsException() {
        String referenceId = "REF-123";

        when(paymentRepository.findByReferenceId(referenceId))
                .thenReturn(Optional.of(new Payment()));

        assertThatThrownBy(() -> paymentService.initiatePayment(
                referenceId,
                PaymentRail.ACH,
                PaymentDirection.OUTBOUND,
                fromAccountId,
                toAccountId,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                "USD",
                "Test payment"
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment with reference ID already exists");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_UpdatesStatusToProcessing() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.processPayment(paymentId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPaymentProcessing(payment);
    }

    @Test
    void processPayment_NotPending_ThrowsException() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(paymentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment is not in PENDING status");
    }

    @Test
    void completePayment_UpdatesStatusToCompleted() {
        Payment payment = createPayment(PaymentStatus.PROCESSING);
        String externalRef = "EXT-REF-123";

        when(clock.instant()).thenReturn(fixedClock.instant());
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.completePayment(paymentId, externalRef);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getExternalReference()).isEqualTo(externalRef);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPaymentCompleted(payment);
    }

    @Test
    void failPayment_UpdatesStatusToFailed() {
        Payment payment = createPayment(PaymentStatus.PROCESSING);
        String failureReason = "Insufficient funds";

        when(clock.instant()).thenReturn(fixedClock.instant());
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.failPayment(paymentId, failureReason);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo(failureReason);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPaymentFailed(payment);
    }

    @Test
    void cancelPayment_UpdatesStatusToCancelled() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Payment result = paymentService.cancelPayment(paymentId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPaymentCancelled(payment);
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setReferenceId("REF-123");
        payment.setRail(PaymentRail.ACH);
        payment.setDirection(PaymentDirection.OUTBOUND);
        payment.setStatus(status);
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountId(toAccountId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        return payment;
    }
}

