package com.banking.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.banking.card.domain.RestrictionAction;
import com.banking.card.domain.GeographicRestriction;
import com.banking.card.domain.MerchantRestriction;
import com.banking.card.repository.AuthorizationRequestRepository;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.GeographicRestrictionRepository;
import com.banking.card.repository.MerchantRestrictionRepository;
import com.banking.card.web.dto.AuthorizationRequestDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceExceptionTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AuthorizationRequestRepository authorizationRequestRepository;

    @Mock
    private MerchantRestrictionRepository merchantRestrictionRepository;

    @Mock
    private GeographicRestrictionRepository geographicRestrictionRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private Card testCard;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        testCard = new Card();
        testCard.setId(cardId);
        testCard.setCustomerId(UUID.randomUUID());
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setType(CardType.DEBIT);
        testCard.setCurrency("USD");
        testCard.setSpendingLimit(BigDecimal.valueOf(10000));
    }

    @Test
    void authorizeThrowsCardRestrictionViolationExceptionForBlockedMerchantCategory() {
        // Given
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        
        MerchantRestriction restriction = new MerchantRestriction();
        restriction.setId(UUID.randomUUID());
        restriction.setCard(testCard);
        restriction.setMerchantCategoryCode("5812");
        restriction.setAction(RestrictionAction.BLOCK);
        
        when(merchantRestrictionRepository.findByCardId(cardId))
                .thenReturn(List.of(restriction));

        AuthorizationRequestDto request = new AuthorizationRequestDto(
                BigDecimal.valueOf(100),
                "USD",
                null,
                "5812",
                "US"
        );

        // When/Then
        assertThatThrownBy(() -> authorizationService.authorizeTransaction(cardId, request))
                .isInstanceOf(CardRestrictionViolationException.class)
                .hasMessageContaining("merchant category restriction")
                .satisfies(ex -> {
                    CardRestrictionViolationException violation = (CardRestrictionViolationException) ex;
                    assertThat(violation.getRestrictionType()).isEqualTo("MERCHANT_CATEGORY");
                    assertThat(violation.getRestrictionValue()).isEqualTo("5812");
                });
    }

    @Test
    void authorizeThrowsCardRestrictionViolationExceptionForBlockedCountry() {
        // Given
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        
        GeographicRestriction restriction = new GeographicRestriction();
        restriction.setId(UUID.randomUUID());
        restriction.setCard(testCard);
        restriction.setCountryCode("CN");
        restriction.setAction(RestrictionAction.BLOCK);
        
        when(geographicRestrictionRepository.findByCardId(cardId))
                .thenReturn(List.of(restriction));

        AuthorizationRequestDto request = new AuthorizationRequestDto(
                BigDecimal.valueOf(100),
                "USD",
                null,
                null,
                "CN"
        );

        // When/Then
        assertThatThrownBy(() -> authorizationService.authorizeTransaction(cardId, request))
                .isInstanceOf(CardRestrictionViolationException.class)
                .hasMessageContaining("geographic restriction")
                .satisfies(ex -> {
                    CardRestrictionViolationException violation = (CardRestrictionViolationException) ex;
                    assertThat(violation.getRestrictionType()).isEqualTo("GEOGRAPHIC");
                    assertThat(violation.getRestrictionValue()).isEqualTo("CN");
                });
    }
}

