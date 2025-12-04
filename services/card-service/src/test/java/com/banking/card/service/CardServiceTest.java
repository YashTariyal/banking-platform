package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.CreateCardRequest;
import com.banking.card.web.dto.UpdateCardLimitRequest;
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

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    @Test
    void issueCardCreatesPendingCard() {
        CreateCardRequest request =
                new CreateCardRequest(customerId, CardType.DEBIT, "USD", new BigDecimal("1000.00"));

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
        return card;
    }
}


