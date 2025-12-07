package com.banking.card.service;

import com.banking.card.domain.CardAnalytics;
import com.banking.card.domain.CardTransaction;
import com.banking.card.domain.TransactionStatus;
import com.banking.card.repository.CardAnalyticsRepository;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.CardTransactionRepository;
import com.banking.card.web.dto.CardAnalyticsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyticsService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository transactionRepository;
    private final CardAnalyticsRepository analyticsRepository;

    public AnalyticsService(
            CardRepository cardRepository,
            CardTransactionRepository transactionRepository,
            CardAnalyticsRepository analyticsRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional(readOnly = true)
    public CardAnalyticsResponse getCardAnalytics(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }

        CardAnalytics analytics = analyticsRepository.findByCardId(cardId)
                .orElseGet(() -> calculateAndSaveAnalytics(cardId));

        return toResponse(analytics);
    }

    public CardAnalyticsResponse refreshAnalytics(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }

        CardAnalytics analytics = calculateAndSaveAnalytics(cardId);
        return toResponse(analytics);
    }

    private CardAnalytics calculateAndSaveAnalytics(UUID cardId) {
        List<CardTransaction> allTransactions = transactionRepository
                .findByCardIdOrderByTransactionDateDesc(cardId,
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        CardAnalytics analytics = analyticsRepository.findByCardId(cardId)
                .orElseGet(() -> {
                    CardAnalytics newAnalytics = new CardAnalytics();
                    newAnalytics.setCardId(cardId);
                    newAnalytics.setCard(cardRepository.getReferenceById(cardId));
                    return newAnalytics;
                });

        int totalTransactions = allTransactions.size();
        BigDecimal totalAmount = allTransactions.stream()
                .map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalTransactions > 0
                ? totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long declinedCount = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.DECLINED)
                .count();

        Instant lastTransactionDate = allTransactions.stream()
                .map(CardTransaction::getTransactionDate)
                .max(Instant::compareTo)
                .orElse(null);

        String topMcc = allTransactions.stream()
                .filter(t -> t.getMerchantCategoryCode() != null)
                .collect(Collectors.groupingBy(CardTransaction::getMerchantCategoryCode, Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);

        String mostUsedCountry = allTransactions.stream()
                .filter(t -> t.getMerchantCountry() != null)
                .collect(Collectors.groupingBy(CardTransaction::getMerchantCountry, Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);

        analytics.setTotalTransactions(totalTransactions);
        analytics.setTotalAmount(totalAmount);
        analytics.setAverageTransactionAmount(averageAmount);
        analytics.setDeclinedTransactions((int) declinedCount);
        analytics.setLastTransactionDate(lastTransactionDate);
        analytics.setTopMerchantCategory(topMcc);
        analytics.setMostUsedCountry(mostUsedCountry);
        analytics.setLastUpdatedAt(Instant.now());

        return analyticsRepository.save(analytics);
    }

    private CardAnalyticsResponse toResponse(CardAnalytics analytics) {
        return new CardAnalyticsResponse(
                analytics.getCardId(),
                analytics.getTotalTransactions(),
                analytics.getTotalAmount(),
                analytics.getAverageTransactionAmount(),
                analytics.getDeclinedTransactions(),
                analytics.getLastTransactionDate(),
                analytics.getTopMerchantCategory(),
                analytics.getMostUsedCountry(),
                analytics.getLastUpdatedAt()
        );
    }
}

