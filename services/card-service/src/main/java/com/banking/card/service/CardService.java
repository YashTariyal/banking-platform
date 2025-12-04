package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.CardMapper;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.CreateCardRequest;
import com.banking.card.web.dto.PageResponse;
import com.banking.card.web.dto.UpdateCardLimitRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public CardResponse issueCard(CreateCardRequest request) {
        validateCurrency(request.currency());
        UUID id = UUID.randomUUID();
        String cardNumber = generateCardNumber(id);
        String masked = maskCardNumber(cardNumber);
        Instant now = Instant.now();

        Card card = new Card();
        card.setId(id);
        card.setCustomerId(request.customerId());
        card.setCardNumber(cardNumber);
        card.setMaskedNumber(masked);
        card.setStatus(CardStatus.PENDING_ACTIVATION);
        card.setType(request.type());
        card.setCurrency(request.currency());
        card.setSpendingLimit(request.spendingLimit());
        card.setCreatedAt(now);
        card.setUpdatedAt(now);

        Card saved = cardRepository.save(card);
        return CardMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(UUID id) {
        return cardRepository.findById(id)
                .map(CardMapper::toResponse)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> listCards(UUID customerId, int page, int size) {
        if (customerId == null) {
            Page<Card> cards = cardRepository.findAll(PageRequest.of(page, size));
            return PageResponse.from(cards.map(CardMapper::toResponse));
        }
        Page<Card> cards = cardRepository.findByCustomerId(customerId, PageRequest.of(page, size));
        return PageResponse.from(cards.map(CardMapper::toResponse));
    }

    public CardResponse activateCard(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() == CardStatus.CANCELLED || card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Cannot activate a cancelled or blocked card");
        }
        card.setStatus(CardStatus.ACTIVE);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse blockCard(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Card is already cancelled");
        }
        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse updateLimit(UUID id, UpdateCardLimitRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        BigDecimal newLimit = request.spendingLimit();
        if (newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Spending limit must be positive");
        }
        card.setSpendingLimit(newLimit);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }

    private String generateCardNumber(UUID id) {
        // Simple deterministic pseudo-card number for now (BIN 400000 + 10 hex chars of UUID)
        String suffix = id.toString().replace("-", "").substring(0, 10);
        return "400000" + suffix;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() <= 4) {
            return cardNumber;
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}


