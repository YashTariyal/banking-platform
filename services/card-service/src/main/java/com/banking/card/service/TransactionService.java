package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardTransaction;
import com.banking.card.domain.TransactionStatus;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.CardTransactionRepository;
import com.banking.card.web.dto.CardTransactionResponse;
import com.banking.card.web.dto.CreateTransactionRequest;
import com.banking.card.web.dto.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository transactionRepository;

    public TransactionService(CardRepository cardRepository, CardTransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    public CardTransactionResponse createTransaction(UUID cardId, CreateTransactionRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Transactions can only be created for active cards");
        }

        CardTransaction transaction = new CardTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setCard(card);
        transaction.setTransactionType(request.transactionType());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency() != null ? request.currency() : card.getCurrency());
        transaction.setMerchantName(request.merchantName());
        transaction.setMerchantCategoryCode(request.merchantCategoryCode());
        transaction.setMerchantCountry(request.merchantCountry());
        transaction.setTransactionDate(Instant.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());

        CardTransaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardTransactionResponse> getTransactionHistory(UUID cardId, int page, int size) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }

        Page<CardTransaction> transactions = transactionRepository.findByCardIdOrderByTransactionDateDesc(
                cardId, PageRequest.of(page, size));
        return PageResponse.from(transactions.map(this::toResponse));
    }

    private CardTransactionResponse toResponse(CardTransaction transaction) {
        return new CardTransactionResponse(
                transaction.getId(),
                transaction.getCard().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantName(),
                transaction.getMerchantCategoryCode(),
                transaction.getMerchantCountry(),
                transaction.getTransactionDate(),
                transaction.getStatus(),
                transaction.getDeclineReason(),
                transaction.getCreatedAt()
        );
    }
}

