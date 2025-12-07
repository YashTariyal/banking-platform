package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.integration.BalanceServiceClient;
import com.banking.card.repository.CardRepository;
import com.banking.card.service.CardNotFoundException;
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
    private final BalanceServiceClient balanceServiceClient;

    public BalanceVerificationService(
            CardRepository cardRepository,
            BalanceServiceClient balanceServiceClient) {
        this.cardRepository = cardRepository;
        this.balanceServiceClient = balanceServiceClient;
    }

    public BalanceVerificationResponse verifyBalance(UUID cardId, BalanceVerificationRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (card.getAccountId() == null) {
            throw new IllegalStateException("Card is not linked to an account");
        }

        String currency = card.getCurrency();
        boolean hasSufficientBalance = balanceServiceClient.hasSufficientBalance(
                card.getAccountId(), request.amount(), currency);
        BigDecimal availableBalance = balanceServiceClient.getAvailableBalance(
                card.getAccountId(), currency);

        return new BalanceVerificationResponse(
                hasSufficientBalance,
                availableBalance,
                request.amount()
        );
    }
}

