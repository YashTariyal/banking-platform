package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.dto.BalanceVerificationRequest;
import com.banking.card.web.dto.BalanceVerificationResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BalanceVerificationService {

    private final CardRepository cardRepository;

    public BalanceVerificationService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public BalanceVerificationResponse verifyBalance(UUID cardId, BalanceVerificationRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // In a real system, this would call the ledger/account service
        // For now, we'll use the spending limit as a proxy for available balance
        // In production, this should integrate with the account service via REST or Kafka
        BigDecimal availableBalance = card.getSpendingLimit() != null 
                ? card.getSpendingLimit() 
                : BigDecimal.ZERO;

        boolean sufficient = availableBalance.compareTo(request.amount()) >= 0;

        return new BalanceVerificationResponse(
                sufficient,
                availableBalance,
                request.amount()
        );
    }
}

