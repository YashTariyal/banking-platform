package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardTransaction;
import com.banking.card.domain.FraudEvent;
import com.banking.card.domain.FraudEventType;
import com.banking.card.domain.FraudSeverity;
import com.banking.card.domain.VelocityTracking;
import com.banking.card.domain.VelocityWindow;
import com.banking.card.events.FraudEventPublisher;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.CardTransactionRepository;
import com.banking.card.repository.FraudEventRepository;
import com.banking.card.repository.VelocityTrackingRepository;
import com.banking.card.web.dto.FraudCheckResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FraudDetectionService {

    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final int MAX_TRANSACTIONS_PER_DAY = 50;
    private static final BigDecimal MAX_AMOUNT_PER_HOUR = new BigDecimal("5000.00");
    private static final BigDecimal MAX_AMOUNT_PER_DAY = new BigDecimal("10000.00");

    private final CardRepository cardRepository;
    private final CardTransactionRepository transactionRepository;
    private final FraudEventRepository fraudEventRepository;
    private final VelocityTrackingRepository velocityTrackingRepository;
    private final FraudEventPublisher fraudEventPublisher;
    private final MeterRegistry meterRegistry;
    private final Counter fraudCheckCounter;

    public FraudDetectionService(
            CardRepository cardRepository,
            CardTransactionRepository transactionRepository,
            FraudEventRepository fraudEventRepository,
            VelocityTrackingRepository velocityTrackingRepository,
            FraudEventPublisher fraudEventPublisher,
            MeterRegistry meterRegistry) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.fraudEventRepository = fraudEventRepository;
        this.velocityTrackingRepository = velocityTrackingRepository;
        this.fraudEventPublisher = fraudEventPublisher;
        this.meterRegistry = meterRegistry;
        this.fraudCheckCounter = meterRegistry.counter("card.fraud.checks");
    }

    public FraudCheckResponse checkForFraud(UUID cardId, BigDecimal amount, String merchantCountry) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));

        fraudCheckCounter.increment();
        List<String> riskFactors = new ArrayList<>();
        BigDecimal fraudScore = BigDecimal.ZERO;
        FraudSeverity severity = FraudSeverity.LOW;

        // Velocity checks
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

        // Check hourly velocity
        VelocityTracking hourTracking = getOrCreateVelocityTracking(cardId, VelocityWindow.HOUR, oneHourAgo);
        if (hourTracking.getTransactionCount() >= MAX_TRANSACTIONS_PER_HOUR) {
            riskFactors.add("Hourly transaction limit exceeded");
            fraudScore = fraudScore.add(new BigDecimal("30"));
            severity = FraudSeverity.HIGH;
        }
        if (hourTracking.getTotalAmount().add(amount).compareTo(MAX_AMOUNT_PER_HOUR) > 0) {
            riskFactors.add("Hourly amount limit exceeded");
            fraudScore = fraudScore.add(new BigDecimal("25"));
            if (severity.ordinal() < FraudSeverity.HIGH.ordinal()) {
                severity = FraudSeverity.HIGH;
            }
        }

        // Check daily velocity
        VelocityTracking dayTracking = getOrCreateVelocityTracking(cardId, VelocityWindow.DAY, oneDayAgo);
        if (dayTracking.getTransactionCount() >= MAX_TRANSACTIONS_PER_DAY) {
            riskFactors.add("Daily transaction limit exceeded");
            fraudScore = fraudScore.add(new BigDecimal("40"));
            severity = FraudSeverity.CRITICAL;
        }
        if (dayTracking.getTotalAmount().add(amount).compareTo(MAX_AMOUNT_PER_DAY) > 0) {
            riskFactors.add("Daily amount limit exceeded");
            fraudScore = fraudScore.add(new BigDecimal("35"));
            if (severity.ordinal() < FraudSeverity.CRITICAL.ordinal()) {
                severity = FraudSeverity.CRITICAL;
            }
        }

        // Check for unusual amount (if amount is significantly higher than average)
        BigDecimal averageAmount = calculateAverageTransactionAmount(cardId);
        if (averageAmount != null && amount.compareTo(averageAmount.multiply(new BigDecimal("3"))) > 0) {
            riskFactors.add("Unusual transaction amount");
            fraudScore = fraudScore.add(new BigDecimal("20"));
            if (severity.ordinal() < FraudSeverity.MEDIUM.ordinal()) {
                severity = FraudSeverity.MEDIUM;
            }
        }

        // Check for unusual location (if card has recent transactions in different countries)
        if (merchantCountry != null && hasRecentTransactionsInDifferentCountry(cardId, merchantCountry)) {
            riskFactors.add("Unusual transaction location");
            fraudScore = fraudScore.add(new BigDecimal("25"));
            if (severity.ordinal() < FraudSeverity.HIGH.ordinal()) {
                severity = FraudSeverity.HIGH;
            }
        }

        List<FraudEvent> unresolvedEvents = fraudEventRepository.findByCardIdAndResolvedFalse(cardId);
        if (!unresolvedEvents.isEmpty()) {
            riskFactors.add("Existing unresolved fraud events");
            fraudScore = fraudScore.add(new BigDecimal("15"));
            if (severity.ordinal() < FraudSeverity.HIGH.ordinal()) {
                severity = FraudSeverity.HIGH;
            }
        }

        // Update velocity tracking
        updateVelocityTracking(hourTracking, amount, card);
        updateVelocityTracking(dayTracking, amount, card);

        boolean isFraudulent = fraudScore.compareTo(new BigDecimal("50")) >= 0;

        if (isFraudulent) {
            createFraudEvent(cardId, FraudEventType.VELOCITY_EXCEEDED, severity,
                    "Fraud detected: " + String.join(", ", riskFactors), fraudScore);
            meterRegistry.counter("card.fraud.detected", "severity", severity.name()).increment();
            fraudEventPublisher.publishFraudDetected(cardId, card.getCustomerId(), severity, fraudScore, riskFactors);
        }

        return new FraudCheckResponse(isFraudulent, fraudScore, severity, riskFactors);
    }

    private VelocityTracking getOrCreateVelocityTracking(UUID cardId, VelocityWindow windowType, Instant windowStart) {
        return velocityTrackingRepository
                .findByCardIdAndWindowTypeAndWindowStart(cardId, windowType, windowStart)
                .orElseGet(() -> {
                    Card card = cardRepository.getReferenceById(cardId);
                    VelocityTracking tracking = new VelocityTracking();
                    tracking.setId(UUID.randomUUID());
                    tracking.setCard(card);
                    tracking.setWindowType(windowType);
                    tracking.setWindowStart(windowStart);
                    tracking.setTransactionCount(0);
                    tracking.setTotalAmount(BigDecimal.ZERO);
                    tracking.setCreatedAt(Instant.now());
                    return velocityTrackingRepository.save(tracking);
                });
    }

    private void updateVelocityTracking(VelocityTracking tracking, BigDecimal amount, Card card) {
        tracking.setTransactionCount(tracking.getTransactionCount() + 1);
        tracking.setTotalAmount(tracking.getTotalAmount().add(amount));
        velocityTrackingRepository.save(tracking);
        meterRegistry.counter("card.fraud.velocity.updated", "cardId", card.getId().toString(),
                "window", tracking.getWindowType().name()).increment();
    }

    private BigDecimal calculateAverageTransactionAmount(UUID cardId) {
        List<CardTransaction> recentTransactions = transactionRepository
                .findByCardIdOrderByTransactionDateDesc(cardId, 
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();
        
        if (recentTransactions.isEmpty()) {
            return null;
        }
        
        BigDecimal total = recentTransactions.stream()
                .map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.divide(new BigDecimal(recentTransactions.size()), 2, 
                java.math.RoundingMode.HALF_UP);
    }

    private boolean hasRecentTransactionsInDifferentCountry(UUID cardId, String currentCountry) {
        List<CardTransaction> recentTransactions = transactionRepository
                .findByCardIdOrderByTransactionDateDesc(cardId,
                        org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent();
        
        return recentTransactions.stream()
                .filter(t -> t.getMerchantCountry() != null)
                .anyMatch(t -> !t.getMerchantCountry().equalsIgnoreCase(currentCountry));
    }

    private void createFraudEvent(UUID cardId, FraudEventType eventType, FraudSeverity severity,
                                   String description, BigDecimal fraudScore) {
        FraudEvent event = new FraudEvent();
        event.setId(UUID.randomUUID());
        event.setCard(cardRepository.getReferenceById(cardId));
        event.setEventType(eventType);
        event.setSeverity(severity);
        event.setDescription(description);
        event.setFraudScore(fraudScore);
        event.setDetectedAt(Instant.now());
        event.setResolved(false);
        event.setCreatedAt(Instant.now());
        fraudEventRepository.save(event);
    }
}

