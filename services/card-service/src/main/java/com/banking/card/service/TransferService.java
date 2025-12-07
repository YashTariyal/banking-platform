package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardTransfer;
import com.banking.card.domain.TransferStatus;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.CardTransferRepository;
import com.banking.card.web.dto.CardTransferRequest;
import com.banking.card.web.dto.CardTransferResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferService {

    private final CardRepository cardRepository;
    private final CardTransferRepository transferRepository;

    public TransferService(CardRepository cardRepository, CardTransferRepository transferRepository) {
        this.cardRepository = cardRepository;
        this.transferRepository = transferRepository;
    }

    public CardTransferResponse transferBetweenCards(UUID fromCardId, CardTransferRequest request) {
        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new CardNotFoundException(fromCardId));
        Card toCard = cardRepository.findById(request.toCardId())
                .orElseThrow(() -> new CardNotFoundException(request.toCardId()));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card must be active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Destination card must be active");
        }
        if (fromCardId.equals(request.toCardId())) {
            throw new IllegalArgumentException("Cannot transfer to the same card");
        }
        if (!fromCard.getCurrency().equals(toCard.getCurrency()) && 
            !fromCard.getCurrency().equals(request.currency()) &&
            !toCard.getCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("Currency mismatch between cards");
        }

        CardTransfer transfer = new CardTransfer();
        transfer.setId(UUID.randomUUID());
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(request.amount());
        transfer.setCurrency(request.currency() != null ? request.currency() : fromCard.getCurrency());
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setTransferDate(Instant.now());
        transfer.setCreatedAt(Instant.now());

        // In a real system, this would integrate with the ledger/account service
        // For now, we'll mark it as completed
        transfer.setStatus(TransferStatus.COMPLETED);

        CardTransfer saved = transferRepository.save(transfer);
        return toResponse(saved);
    }

    private CardTransferResponse toResponse(CardTransfer transfer) {
        return new CardTransferResponse(
                transfer.getId(),
                transfer.getFromCard().getId(),
                transfer.getToCard().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getTransferDate(),
                transfer.getFailureReason(),
                transfer.getCreatedAt()
        );
    }
}

