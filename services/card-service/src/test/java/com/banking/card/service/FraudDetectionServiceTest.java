package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardTransaction;
import com.banking.card.domain.FraudEvent;
import com.banking.card.domain.FraudSeverity;
import com.banking.card.domain.VelocityTracking;
import com.banking.card.domain.VelocityWindow;
import com.banking.card.events.FraudEventPublisher;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.CardTransactionRepository;
import com.banking.card.repository.FraudEventRepository;
import com.banking.card.repository.VelocityTrackingRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardTransactionRepository transactionRepository;

    @Mock
    private FraudEventRepository fraudEventRepository;

    @Mock
    private VelocityTrackingRepository velocityTrackingRepository;

    @Mock
    private FraudEventPublisher fraudEventPublisher;

    private MeterRegistry meterRegistry;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        fraudDetectionService = new FraudDetectionService(
                cardRepository,
                transactionRepository,
                fraudEventRepository,
                velocityTrackingRepository,
                fraudEventPublisher,
                meterRegistry);
    }

    @Test
    void publishesEventAndMetricsWhenFraudDetected() {
        UUID cardId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Card card = new Card();
        card.setId(cardId);
        card.setCustomerId(customerId);

        VelocityTracking hourTracking = new VelocityTracking();
        hourTracking.setId(UUID.randomUUID());
        hourTracking.setWindowType(VelocityWindow.HOUR);
        hourTracking.setWindowStart(Instant.now().minusSeconds(3600));
        hourTracking.setTransactionCount(10); // triggers hourly threshold
        hourTracking.setTotalAmount(new BigDecimal("6000.00")); // triggers hourly amount threshold

        VelocityTracking dayTracking = new VelocityTracking();
        dayTracking.setId(UUID.randomUUID());
        dayTracking.setWindowType(VelocityWindow.DAY);
        dayTracking.setWindowStart(Instant.now().minusSeconds(86400));
        dayTracking.setTransactionCount(1);
        dayTracking.setTotalAmount(new BigDecimal("100.00"));

        CardTransaction sampleTxn = new CardTransaction();
        sampleTxn.setAmount(new BigDecimal("50.00"));

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(transactionRepository.findByCardIdOrderByTransactionDateDesc(eq(cardId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTxn)));
        when(velocityTrackingRepository.findByCardIdAndWindowTypeAndWindowStart(eq(cardId), eq(VelocityWindow.HOUR), any()))
                .thenReturn(Optional.of(hourTracking));
        when(velocityTrackingRepository.findByCardIdAndWindowTypeAndWindowStart(eq(cardId), eq(VelocityWindow.DAY), any()))
                .thenReturn(Optional.of(dayTracking));
        when(fraudEventRepository.findByCardIdAndResolvedFalse(cardId))
                .thenReturn(List.of(new FraudEvent()));
        when(velocityTrackingRepository.save(any(VelocityTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fraudDetectionService.checkForFraud(cardId, new BigDecimal("200.00"), "US");

        verify(fraudEventPublisher).publishFraudDetected(
                eq(cardId),
                eq(customerId),
                eq(FraudSeverity.HIGH),
                any(BigDecimal.class),
                any());

        assertThat(meterRegistry.counter("card.fraud.checks").count()).isEqualTo(1.0d);
        assertThat(meterRegistry.find("card.fraud.detected").tags("severity", "HIGH").counter()).isNotNull();
        assertThat(meterRegistry.find("card.fraud.velocity.updated").tags("cardId", cardId.toString()).counter()).isNotNull();
    }
}

