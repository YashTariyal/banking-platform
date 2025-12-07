package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.banking.card.integration.BalanceServiceClient;
import com.banking.card.repository.CardRepository;
import com.banking.card.web.dto.BalanceVerificationRequest;
import com.banking.card.web.dto.BalanceVerificationResponse;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceVerificationServiceIntegrationTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BalanceServiceClient balanceServiceClient;

    @InjectMocks
    private BalanceVerificationService balanceVerificationService;

    private Card testCard;
    private UUID cardId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        
        testCard = new Card();
        testCard.setId(cardId);
        testCard.setCustomerId(UUID.randomUUID());
        testCard.setAccountId(accountId);
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setType(CardType.DEBIT);
        testCard.setCurrency("USD");
    }

    @Test
    void verifyBalanceUsesBalanceServiceClient() {
        // Given
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(balanceServiceClient.hasSufficientBalance(eq(accountId), any(BigDecimal.class), eq("USD")))
                .thenReturn(true);
        when(balanceServiceClient.getAvailableBalance(eq(accountId), eq("USD")))
                .thenReturn(BigDecimal.valueOf(5000));

        BalanceVerificationRequest request = new BalanceVerificationRequest(BigDecimal.valueOf(1000));

        // When
        BalanceVerificationResponse response = balanceVerificationService.verifyBalance(cardId, request);

        // Then
        assertThat(response.sufficient()).isTrue();
        assertThat(response.availableBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(response.requestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void verifyBalanceThrowsExceptionWhenCardNotLinkedToAccount() {
        // Given
        testCard.setAccountId(null);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        BalanceVerificationRequest request = new BalanceVerificationRequest(BigDecimal.valueOf(1000));

        // When/Then
        assertThatThrownBy(() -> balanceVerificationService.verifyBalance(cardId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not linked to an account");
    }

    @Test
    void verifyBalanceThrowsExceptionWhenCardNotFound() {
        // Given
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        BalanceVerificationRequest request = new BalanceVerificationRequest(BigDecimal.valueOf(1000));

        // When/Then
        assertThatThrownBy(() -> balanceVerificationService.verifyBalance(cardId, request))
                .isInstanceOf(CardNotFoundException.class);
    }
}

