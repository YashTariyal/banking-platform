package com.banking.card.service;

import com.banking.card.domain.AuthorizationRequest;
import com.banking.card.domain.AuthorizationStatus;
import com.banking.card.domain.Card;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.GeographicRestriction;
import com.banking.card.domain.MerchantRestriction;
import com.banking.card.domain.RestrictionAction;
import com.banking.card.repository.AuthorizationRequestRepository;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.GeographicRestrictionRepository;
import com.banking.card.repository.MerchantRestrictionRepository;
import com.banking.card.web.dto.AuthorizationRequestDto;
import com.banking.card.web.dto.AuthorizationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorizationService {

    private final CardRepository cardRepository;
    private final AuthorizationRequestRepository authorizationRepository;
    private final MerchantRestrictionRepository merchantRestrictionRepository;
    private final GeographicRestrictionRepository geographicRestrictionRepository;

    public AuthorizationService(
            CardRepository cardRepository,
            AuthorizationRequestRepository authorizationRepository,
            MerchantRestrictionRepository merchantRestrictionRepository,
            GeographicRestrictionRepository geographicRestrictionRepository) {
        this.cardRepository = cardRepository;
        this.authorizationRepository = authorizationRepository;
        this.merchantRestrictionRepository = merchantRestrictionRepository;
        this.geographicRestrictionRepository = geographicRestrictionRepository;
    }

    public AuthorizationResponse authorizeTransaction(UUID cardId, AuthorizationRequestDto request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        AuthorizationRequest authorization = new AuthorizationRequest();
        authorization.setId(UUID.randomUUID());
        authorization.setCard(card);
        authorization.setAmount(request.amount());
        authorization.setCurrency(request.currency() != null ? request.currency() : card.getCurrency());
        authorization.setMerchantName(request.merchantName());
        authorization.setMerchantCategoryCode(request.merchantCategoryCode());
        authorization.setMerchantCountry(request.merchantCountry());
        authorization.setCheckedAt(Instant.now());
        authorization.setCreatedAt(Instant.now());

        // Perform authorization checks
        AuthorizationStatus status = performAuthorizationChecks(card, request);
        authorization.setAuthorizationStatus(status);

        if (status == AuthorizationStatus.DECLINED) {
            authorization.setDeclineReason(determineDeclineReason(card, request));
        }

        AuthorizationRequest saved = authorizationRepository.save(authorization);
        return toResponse(saved);
    }

    private AuthorizationStatus performAuthorizationChecks(Card card, AuthorizationRequestDto request) {
        // Check card status
        if (card.getStatus() != CardStatus.ACTIVE) {
            return AuthorizationStatus.DECLINED;
        }

        // Check if card is frozen
        if (Boolean.TRUE.equals(card.getFrozen())) {
            return AuthorizationStatus.DECLINED;
        }

        // Check spending limits
        if (card.getSpendingLimit() != null && request.amount().compareTo(card.getSpendingLimit()) > 0) {
            return AuthorizationStatus.DECLINED;
        }

        // Check merchant category restrictions
        if (request.merchantCategoryCode() != null) {
            List<MerchantRestriction> restrictions = merchantRestrictionRepository.findByCardId(card.getId());
            for (MerchantRestriction restriction : restrictions) {
                if (restriction.getMerchantCategoryCode().equals(request.merchantCategoryCode())) {
                    if (restriction.getAction() == RestrictionAction.BLOCK) {
                        return AuthorizationStatus.DECLINED;
                    }
                }
            }
        }

        // Check geographic restrictions
        if (request.merchantCountry() != null) {
            List<GeographicRestriction> restrictions = geographicRestrictionRepository.findByCardId(card.getId());
            for (GeographicRestriction restriction : restrictions) {
                if (restriction.getCountryCode().equalsIgnoreCase(request.merchantCountry())) {
                    if (restriction.getAction() == RestrictionAction.BLOCK) {
                        return AuthorizationStatus.DECLINED;
                    }
                }
            }
        }

        return AuthorizationStatus.APPROVED;
    }

    private String determineDeclineReason(Card card, AuthorizationRequestDto request) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            return "Card is not active";
        }
        if (Boolean.TRUE.equals(card.getFrozen())) {
            return "Card is frozen";
        }
        if (card.getSpendingLimit() != null && request.amount().compareTo(card.getSpendingLimit()) > 0) {
            return "Amount exceeds spending limit";
        }
        if (request.merchantCategoryCode() != null) {
            List<MerchantRestriction> restrictions = merchantRestrictionRepository.findByCardId(card.getId());
            for (MerchantRestriction restriction : restrictions) {
                if (restriction.getMerchantCategoryCode().equals(request.merchantCategoryCode()) &&
                    restriction.getAction() == RestrictionAction.BLOCK) {
                    return "Merchant category blocked";
                }
            }
        }
        if (request.merchantCountry() != null) {
            List<GeographicRestriction> restrictions = geographicRestrictionRepository.findByCardId(card.getId());
            for (GeographicRestriction restriction : restrictions) {
                if (restriction.getCountryCode().equalsIgnoreCase(request.merchantCountry()) &&
                    restriction.getAction() == RestrictionAction.BLOCK) {
                    return "Country blocked";
                }
            }
        }
        return "Authorization declined";
    }

    private AuthorizationResponse toResponse(AuthorizationRequest authorization) {
        return new AuthorizationResponse(
                authorization.getId(),
                authorization.getCard().getId(),
                authorization.getAmount(),
                authorization.getCurrency(),
                authorization.getAuthorizationStatus(),
                authorization.getDeclineReason(),
                authorization.getCheckedAt()
        );
    }
}

