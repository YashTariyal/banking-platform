package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.CardMapper;
import com.banking.card.web.dto.CancelCardRequest;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.ChangePinRequest;
import com.banking.card.web.dto.CreateCardRequest;
import com.banking.card.web.dto.FreezeCardRequest;
import com.banking.card.web.dto.PageResponse;
import com.banking.card.web.dto.ReplaceCardRequest;
import com.banking.card.web.dto.SetPinRequest;
import com.banking.card.web.dto.UpdateAccountLinkRequest;
import com.banking.card.web.dto.UpdateAtmLimitsRequest;
import com.banking.card.web.dto.UpdateCardLimitRequest;
import com.banking.card.web.dto.UpdateTransactionLimitsRequest;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final int PIN_LOCK_DURATION_MINUTES = 30;
    private static final int DEFAULT_CARD_VALIDITY_YEARS = 3;
    private static final int CVV_ROTATION_MONTHS = 12; // CVV rotates every 12 months

    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    public CardService(CardRepository cardRepository, PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
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
        card.setIssuedAt(now);
        card.setExpirationDate(LocalDate.now().plusYears(DEFAULT_CARD_VALIDITY_YEARS));
        card.setFrozen(false);
        card.setPinAttempts(0);
        card.setIsReplacement(false);
        card.setRenewalCount(0);
        card.setContactlessEnabled(true);
        
        // Set account ID and cardholder name if provided
        if (request.accountId() != null) {
            card.setAccountId(request.accountId());
        }
        if (request.cardholderName() != null && !request.cardholderName().isEmpty()) {
            card.setCardholderName(request.cardholderName());
        }
        
        // Generate CVV
        generateAndSetCvv(card);

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

    public CardResponse cancelCard(UUID id, CancelCardRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Card is already cancelled");
        }
        card.setStatus(CardStatus.CANCELLED);
        card.setCancellationReason(request.reason());
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // Transaction Limits
    public CardResponse updateTransactionLimits(UUID id, UpdateTransactionLimitsRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setDailyTransactionLimit(request.dailyLimit());
        card.setMonthlyTransactionLimit(request.monthlyLimit());
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // PIN Management
    public CardResponse setPin(UUID id, SetPinRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("PIN can only be set for active cards");
        }
        if (isPinLocked(card)) {
            throw new IllegalStateException("PIN is locked. Please try again later");
        }
        
        String pinHash = passwordEncoder.encode(request.pin());
        card.setPinHash(pinHash);
        card.setPinAttempts(0);
        card.setPinLockedUntil(null);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse changePin(UUID id, ChangePinRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("PIN can only be changed for active cards");
        }
        if (isPinLocked(card)) {
            throw new IllegalStateException("PIN is locked. Please try again later");
        }
        if (card.getPinHash() == null || card.getPinHash().isEmpty()) {
            throw new IllegalStateException("PIN not set. Please set PIN first");
        }
        
        // Verify current PIN
        if (!passwordEncoder.matches(request.currentPin(), card.getPinHash())) {
            incrementPinAttempts(card);
            throw new IllegalArgumentException("Current PIN is incorrect");
        }
        
        // Set new PIN
        String newPinHash = passwordEncoder.encode(request.newPin());
        card.setPinHash(newPinHash);
        card.setPinAttempts(0);
        card.setPinLockedUntil(null);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse resetPinAttempts(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setPinAttempts(0);
        card.setPinLockedUntil(null);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // Freeze/Unfreeze
    public CardResponse freezeCard(UUID id, FreezeCardRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot freeze a cancelled card");
        }
        if (Boolean.TRUE.equals(card.getFrozen())) {
            throw new IllegalStateException("Card is already frozen");
        }
        
        card.setFrozen(true);
        card.setFrozenAt(Instant.now());
        card.setFrozenReason(request.reason());
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse unfreezeCard(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (!Boolean.TRUE.equals(card.getFrozen())) {
            throw new IllegalStateException("Card is not frozen");
        }
        
        card.setFrozen(false);
        card.setFrozenAt(null);
        card.setFrozenReason(null);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // Card Replacement
    public CardResponse replaceCard(UUID id, ReplaceCardRequest request) {
        Card oldCard = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (oldCard.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot replace a cancelled card");
        }
        
        // Create new card
        UUID newCardId = UUID.randomUUID();
        String newCardNumber = generateCardNumber(newCardId);
        String masked = maskCardNumber(newCardNumber);
        Instant now = Instant.now();
        
        Card newCard = new Card();
        newCard.setId(newCardId);
        newCard.setCustomerId(oldCard.getCustomerId());
        newCard.setCardNumber(newCardNumber);
        newCard.setMaskedNumber(masked);
        newCard.setStatus(CardStatus.PENDING_ACTIVATION);
        newCard.setType(oldCard.getType());
        newCard.setCurrency(oldCard.getCurrency());
        newCard.setSpendingLimit(oldCard.getSpendingLimit());
        newCard.setDailyTransactionLimit(oldCard.getDailyTransactionLimit());
        newCard.setMonthlyTransactionLimit(oldCard.getMonthlyTransactionLimit());
        newCard.setCreatedAt(now);
        newCard.setUpdatedAt(now);
        newCard.setIssuedAt(now);
        newCard.setExpirationDate(LocalDate.now().plusYears(DEFAULT_CARD_VALIDITY_YEARS));
        newCard.setFrozen(false);
        newCard.setIsReplacement(true);
        newCard.setReplacementReason(request.reason());
        
        // Link old card to new card
        oldCard.setReplacedByCardId(newCardId);
        oldCard.setStatus(CardStatus.CANCELLED);
        oldCard.setCancellationReason("Replaced by new card");
        oldCard.setUpdatedAt(now);
        
        cardRepository.save(oldCard);
        Card saved = cardRepository.save(newCard);
        return CardMapper.toResponse(saved);
    }

    // Account Linking
    public CardResponse updateAccountLink(UUID id, UpdateAccountLinkRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setAccountId(request.accountId());
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse unlinkAccount(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setAccountId(null);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // CVV Management
    public CardResponse rotateCvv(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("CVV can only be rotated for active cards");
        }
        
        generateAndSetCvv(card);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // ATM Withdrawal Limits
    public CardResponse updateAtmLimits(UUID id, UpdateAtmLimitsRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setDailyAtmLimit(request.dailyLimit());
        card.setMonthlyAtmLimit(request.monthlyLimit());
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    // Card Renewal
    public CardResponse renewCard(UUID id) {
        Card oldCard = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (oldCard.getStatus() == CardStatus.CANCELLED) {
            throw new IllegalStateException("Cannot renew a cancelled card");
        }
        
        // Create new card with same details
        UUID newCardId = UUID.randomUUID();
        String newCardNumber = generateCardNumber(newCardId);
        String masked = maskCardNumber(newCardNumber);
        Instant now = Instant.now();
        
        Card newCard = new Card();
        newCard.setId(newCardId);
        newCard.setCustomerId(oldCard.getCustomerId());
        newCard.setAccountId(oldCard.getAccountId());
        newCard.setCardNumber(newCardNumber);
        newCard.setMaskedNumber(masked);
        newCard.setStatus(CardStatus.PENDING_ACTIVATION);
        newCard.setType(oldCard.getType());
        newCard.setCurrency(oldCard.getCurrency());
        newCard.setSpendingLimit(oldCard.getSpendingLimit());
        newCard.setDailyTransactionLimit(oldCard.getDailyTransactionLimit());
        newCard.setMonthlyTransactionLimit(oldCard.getMonthlyTransactionLimit());
        newCard.setDailyAtmLimit(oldCard.getDailyAtmLimit());
        newCard.setMonthlyAtmLimit(oldCard.getMonthlyAtmLimit());
        newCard.setCardholderName(oldCard.getCardholderName());
        newCard.setCreatedAt(now);
        newCard.setUpdatedAt(now);
        newCard.setIssuedAt(now);
        newCard.setExpirationDate(LocalDate.now().plusYears(DEFAULT_CARD_VALIDITY_YEARS));
        newCard.setFrozen(false);
        newCard.setPinAttempts(0);
        newCard.setIsReplacement(false);
        newCard.setRenewalCount((oldCard.getRenewalCount() != null ? oldCard.getRenewalCount() : 0) + 1);
        newCard.setRenewedFromCardId(oldCard.getId());
        newCard.setLastRenewedAt(now);
        
        // Generate new CVV
        generateAndSetCvv(newCard);
        
        // Cancel old card
        oldCard.setStatus(CardStatus.CANCELLED);
        oldCard.setCancellationReason("Renewed - replaced by new card");
        oldCard.setUpdatedAt(now);
        
        cardRepository.save(oldCard);
        Card saved = cardRepository.save(newCard);
        return CardMapper.toResponse(saved);
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

    private boolean isPinLocked(Card card) {
        return card.getPinLockedUntil() != null 
                && card.getPinLockedUntil().isAfter(Instant.now());
    }

    private void incrementPinAttempts(Card card) {
        int attempts = (card.getPinAttempts() != null ? card.getPinAttempts() : 0) + 1;
        card.setPinAttempts(attempts);
        
        if (attempts >= MAX_PIN_ATTEMPTS) {
            card.setPinLockedUntil(Instant.now().plusSeconds(PIN_LOCK_DURATION_MINUTES * 60L));
        }
    }

    private void generateAndSetCvv(Card card) {
        // Generate 3-digit CVV
        SecureRandom random = new SecureRandom();
        int cvv = 100 + random.nextInt(900); // 100-999
        String cvvString = String.format("%03d", cvv);
        
        // Hash CVV (similar to PIN)
        String cvvHash = passwordEncoder.encode(cvvString);
        
        Instant now = Instant.now();
        card.setCvvHash(cvvHash);
        card.setCvvGeneratedAt(now);
        card.setCvvRotationDueDate(now.plusSeconds(CVV_ROTATION_MONTHS * 30L * 24L * 60L * 60L));
    }

    // Contactless Payment Controls
    public CardResponse enableContactless(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setContactlessEnabled(true);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }

    public CardResponse disableContactless(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        card.setContactlessEnabled(false);
        card.setUpdatedAt(Instant.now());
        return CardMapper.toResponse(cardRepository.save(card));
    }
}


