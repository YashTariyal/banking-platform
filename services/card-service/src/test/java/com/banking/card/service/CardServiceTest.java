package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.banking.card.events.CardEventPublisher;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.dto.CancelCardRequest;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.ChangePinRequest;
import com.banking.card.web.dto.CreateCardRequest;
import com.banking.card.web.dto.FreezeCardRequest;
import com.banking.card.web.dto.ReplaceCardRequest;
import com.banking.card.web.dto.SetPinRequest;
import com.banking.card.web.dto.UpdateCardLimitRequest;
import com.banking.card.web.dto.UpdateTransactionLimitsRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CardEventPublisher eventPublisher;

    @InjectMocks
    private CardService cardService;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        // Default password encoder behavior (lenient for tests that don't use it)
        lenient().when(passwordEncoder.encode(any(String.class))).thenAnswer(invocation -> "hashed_" + invocation.getArgument(0));
        lenient().when(passwordEncoder.matches(any(String.class), any(String.class))).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            String encoded = invocation.getArgument(1);
            return encoded.equals("hashed_" + raw);
        });
    }

    @Test
    void issueCardCreatesPendingCard() {
        CreateCardRequest request =
                new CreateCardRequest(customerId, CardType.DEBIT, "USD", new BigDecimal("1000.00"), null, null);

        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setCreatedAt(Instant.now());
            card.setUpdatedAt(Instant.now());
            return card;
        });

        CardResponse response = cardService.issueCard(request);

        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.status()).isEqualTo(CardStatus.PENDING_ACTIVATION);
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.spendingLimit()).isEqualByComparingTo("1000.00");
        assertThat(response.maskedNumber()).startsWith("****");
    }

    @Test
    void getCardThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(id))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void activateCardMovesToActive() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.PENDING_ACTIVATION);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.activateCard(id);

        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void activateCardFailsWhenBlockedOrCancelled() {
        UUID id = UUID.randomUUID();
        Card blocked = buildCard(id, CardStatus.BLOCKED);
        when(cardRepository.findById(id)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> cardService.activateCard(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot activate");
    }

    @Test
    void blockCardMovesToBlocked() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.blockCard(id);

        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void updateLimitValidatesPositiveLimit() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCardLimitRequest request = new UpdateCardLimitRequest(new BigDecimal("500.00"));

        CardResponse response = cardService.updateLimit(id, request);

        assertThat(response.spendingLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void updateLimitRejectsNonPositiveLimit() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        UpdateCardLimitRequest request = new UpdateCardLimitRequest(new BigDecimal("0.00"));

        assertThatThrownBy(() -> cardService.updateLimit(id, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelCardMovesToCancelled() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelCardRequest request = new CancelCardRequest("Customer requested cancellation");

        CardResponse response = cardService.cancelCard(id, request);

        assertThat(response.status()).isEqualTo(CardStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Customer requested cancellation");
    }

    @Test
    void cancelCardFailsWhenAlreadyCancelled() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.CANCELLED);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        CancelCardRequest request = new CancelCardRequest("Duplicate cancellation");

        assertThatThrownBy(() -> cardService.cancelCard(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    // Transaction Limits Tests
    @Test
    void updateTransactionLimitsUpdatesBothLimits() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTransactionLimitsRequest request = new UpdateTransactionLimitsRequest(
                new BigDecimal("500.00"),
                new BigDecimal("5000.00")
        );

        CardResponse response = cardService.updateTransactionLimits(id, request);

        assertThat(response.dailyTransactionLimit()).isEqualByComparingTo("500.00");
        assertThat(response.monthlyTransactionLimit()).isEqualByComparingTo("5000.00");
    }

    // PIN Management Tests
    @Test
    void setPinHashesPinAndSetsStatus() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SetPinRequest request = new SetPinRequest("1234");

        CardResponse response = cardService.setPin(id, request);

        assertThat(response.pinSet()).isTrue();
        assertThat(response.pinLocked()).isFalse();
    }

    @Test
    void setPinFailsWhenCardNotActive() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.PENDING_ACTIVATION);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        SetPinRequest request = new SetPinRequest("1234");

        assertThatThrownBy(() -> cardService.setPin(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active");
    }

    @Test
    void changePinVerifiesCurrentPin() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setPinHash("hashed_1234");
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangePinRequest request = new ChangePinRequest("1234", "5678");

        CardResponse response = cardService.changePin(id, request);

        assertThat(response.pinSet()).isTrue();
    }

    @Test
    void changePinFailsWithIncorrectCurrentPin() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setPinHash("hashed_1234");
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangePinRequest request = new ChangePinRequest("9999", "5678");

        assertThatThrownBy(() -> cardService.changePin(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void changePinIncrementsAttemptsOnFailure() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setPinHash("hashed_1234");
        card.setPinAttempts(2); // Already 2 failed attempts
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card saved = invocation.getArgument(0);
            // Verify attempts were incremented
            assertThat(saved.getPinAttempts()).isEqualTo(3);
            assertThat(saved.getPinLockedUntil()).isNotNull();
            return saved;
        });

        ChangePinRequest request = new ChangePinRequest("9999", "5678");

        assertThatThrownBy(() -> cardService.changePin(id, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPinAttemptsClearsLock() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setPinAttempts(3);
        card.setPinLockedUntil(Instant.now().plusSeconds(1800));
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.resetPinAttempts(id);

        assertThat(response.pinLocked()).isFalse();
    }

    // Freeze/Unfreeze Tests
    @Test
    void freezeCardSetsFrozenStatus() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FreezeCardRequest request = new FreezeCardRequest("Suspicious activity detected");

        CardResponse response = cardService.freezeCard(id, request);

        assertThat(response.frozen()).isTrue();
        assertThat(response.frozenReason()).isEqualTo("Suspicious activity detected");
    }

    @Test
    void freezeCardFailsWhenAlreadyFrozen() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setFrozen(true);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        FreezeCardRequest request = new FreezeCardRequest("Another reason");

        assertThatThrownBy(() -> cardService.freezeCard(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already frozen");
    }

    @Test
    void unfreezeCardClearsFrozenStatus() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        card.setFrozen(true);
        card.setFrozenAt(Instant.now());
        card.setFrozenReason("Test freeze");
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.unfreezeCard(id);

        assertThat(response.frozen()).isFalse();
        assertThat(response.frozenReason()).isNull();
    }

    @Test
    void unfreezeCardFailsWhenNotFrozen() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.ACTIVE);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.unfreezeCard(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not frozen");
    }

    // Card Replacement Tests
    @Test
    void replaceCardCreatesNewCardAndCancelsOld() {
        UUID oldCardId = UUID.randomUUID();
        Card oldCard = buildCard(oldCardId, CardStatus.ACTIVE);
        oldCard.setDailyTransactionLimit(new BigDecimal("500.00"));
        oldCard.setMonthlyTransactionLimit(new BigDecimal("5000.00"));
        when(cardRepository.findById(oldCardId)).thenReturn(Optional.of(oldCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReplaceCardRequest request = new ReplaceCardRequest("Lost card");

        CardResponse response = cardService.replaceCard(oldCardId, request);

        assertThat(response.status()).isEqualTo(CardStatus.PENDING_ACTIVATION);
        assertThat(response.isReplacement()).isTrue();
        assertThat(response.replacementReason()).isEqualTo("Lost card");
        assertThat(response.dailyTransactionLimit()).isEqualByComparingTo("500.00");
        assertThat(response.monthlyTransactionLimit()).isEqualByComparingTo("5000.00");
    }

    @Test
    void replaceCardFailsWhenCardCancelled() {
        UUID id = UUID.randomUUID();
        Card card = buildCard(id, CardStatus.CANCELLED);
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));

        ReplaceCardRequest request = new ReplaceCardRequest("Test");

        assertThatThrownBy(() -> cardService.replaceCard(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled");
    }

    // Expiration Date Tests
    @Test
    void issueCardSetsExpirationDate() {
        CreateCardRequest request =
                new CreateCardRequest(customerId, CardType.DEBIT, "USD", new BigDecimal("1000.00"), null, null);

        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setCreatedAt(Instant.now());
            card.setUpdatedAt(Instant.now());
            return card;
        });

        CardResponse response = cardService.issueCard(request);

        assertThat(response.expirationDate()).isNotNull();
        assertThat(response.issuedAt()).isNotNull();
    }

    private Card buildCard(UUID id, CardStatus status) {
        Card card = new Card();
        card.setId(id);
        card.setCustomerId(customerId);
        card.setCardNumber("4000001234567890");
        card.setMaskedNumber("**** **** **** 7890");
        card.setStatus(status);
        card.setType(CardType.DEBIT);
        card.setCurrency("USD");
        card.setSpendingLimit(new BigDecimal("1000.00"));
        card.setCreatedAt(Instant.now());
        card.setUpdatedAt(Instant.now());
        card.setFrozen(false);
        card.setPinAttempts(0);
        card.setIsReplacement(false);
        return card;
    }
}


