package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.domain.GeographicRestriction;
import com.banking.card.domain.MerchantRestriction;
import com.banking.card.repository.CardRepository;
import com.banking.card.repository.GeographicRestrictionRepository;
import com.banking.card.repository.MerchantRestrictionRepository;
import com.banking.card.web.dto.GeographicRestrictionRequest;
import com.banking.card.web.dto.GeographicRestrictionResponse;
import com.banking.card.web.dto.MerchantRestrictionRequest;
import com.banking.card.web.dto.MerchantRestrictionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RestrictionService {

    private final CardRepository cardRepository;
    private final MerchantRestrictionRepository merchantRestrictionRepository;
    private final GeographicRestrictionRepository geographicRestrictionRepository;

    public RestrictionService(
            CardRepository cardRepository,
            MerchantRestrictionRepository merchantRestrictionRepository,
            GeographicRestrictionRepository geographicRestrictionRepository) {
        this.cardRepository = cardRepository;
        this.merchantRestrictionRepository = merchantRestrictionRepository;
        this.geographicRestrictionRepository = geographicRestrictionRepository;
    }

    // Merchant Restrictions
    public MerchantRestrictionResponse addMerchantRestriction(UUID cardId, MerchantRestrictionRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        MerchantRestriction restriction = new MerchantRestriction();
        restriction.setId(UUID.randomUUID());
        restriction.setCard(card);
        restriction.setMerchantCategoryCode(request.merchantCategoryCode());
        restriction.setAction(request.action());
        restriction.setCreatedAt(Instant.now());

        MerchantRestriction saved = merchantRestrictionRepository.save(restriction);
        return toMerchantResponse(saved);
    }

    public void removeMerchantRestriction(UUID cardId, String merchantCategoryCode) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        merchantRestrictionRepository.findByCardIdAndMerchantCategoryCode(cardId, merchantCategoryCode)
                .ifPresent(merchantRestrictionRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<MerchantRestrictionResponse> getMerchantRestrictions(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        return merchantRestrictionRepository.findByCardId(cardId).stream()
                .map(this::toMerchantResponse)
                .toList();
    }

    // Geographic Restrictions
    public GeographicRestrictionResponse addGeographicRestriction(UUID cardId, GeographicRestrictionRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        GeographicRestriction restriction = new GeographicRestriction();
        restriction.setId(UUID.randomUUID());
        restriction.setCard(card);
        restriction.setCountryCode(request.countryCode().toUpperCase());
        restriction.setAction(request.action());
        restriction.setCreatedAt(Instant.now());

        GeographicRestriction saved = geographicRestrictionRepository.save(restriction);
        return toGeographicResponse(saved);
    }

    public void removeGeographicRestriction(UUID cardId, String countryCode) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        geographicRestrictionRepository.findByCardIdAndCountryCode(cardId, countryCode.toUpperCase())
                .ifPresent(geographicRestrictionRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<GeographicRestrictionResponse> getGeographicRestrictions(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        return geographicRestrictionRepository.findByCardId(cardId).stream()
                .map(this::toGeographicResponse)
                .toList();
    }

    private MerchantRestrictionResponse toMerchantResponse(MerchantRestriction restriction) {
        return new MerchantRestrictionResponse(
                restriction.getId(),
                restriction.getCard().getId(),
                restriction.getMerchantCategoryCode(),
                restriction.getAction(),
                restriction.getCreatedAt()
        );
    }

    private GeographicRestrictionResponse toGeographicResponse(GeographicRestriction restriction) {
        return new GeographicRestrictionResponse(
                restriction.getId(),
                restriction.getCard().getId(),
                restriction.getCountryCode(),
                restriction.getAction(),
                restriction.getCreatedAt()
        );
    }
}

