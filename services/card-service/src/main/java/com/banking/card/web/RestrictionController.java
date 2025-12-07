package com.banking.card.web;

import com.banking.card.service.RestrictionService;
import com.banking.card.web.dto.GeographicRestrictionRequest;
import com.banking.card.web.dto.GeographicRestrictionResponse;
import com.banking.card.web.dto.MerchantRestrictionRequest;
import com.banking.card.web.dto.MerchantRestrictionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/restrictions")
@Tag(name = "Card Restrictions", description = "Card merchant and geographic restriction operations")
public class RestrictionController {

    private final RestrictionService restrictionService;

    public RestrictionController(RestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    // Merchant Restrictions
    @PostMapping("/merchant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add merchant category restriction",
            description = "Adds a merchant category code (MCC) restriction to a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restriction added",
                    content = @Content(schema = @Schema(implementation = MerchantRestrictionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public MerchantRestrictionResponse addMerchantRestriction(
            @PathVariable UUID cardId,
            @Valid @RequestBody MerchantRestrictionRequest request
    ) {
        return restrictionService.addMerchantRestriction(cardId, request);
    }

    @GetMapping("/merchant")
    @Operation(
            summary = "Get merchant restrictions",
            description = "Retrieves all merchant category restrictions for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restrictions retrieved"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public List<MerchantRestrictionResponse> getMerchantRestrictions(@PathVariable UUID cardId) {
        return restrictionService.getMerchantRestrictions(cardId);
    }

    @DeleteMapping("/merchant/{merchantCategoryCode}")
    @Operation(
            summary = "Remove merchant restriction",
            description = "Removes a merchant category restriction from a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Restriction removed"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public void removeMerchantRestriction(
            @PathVariable UUID cardId,
            @PathVariable String merchantCategoryCode
    ) {
        restrictionService.removeMerchantRestriction(cardId, merchantCategoryCode);
    }

    // Geographic Restrictions
    @PostMapping("/geographic")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add geographic restriction",
            description = "Adds a country restriction to a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restriction added",
                    content = @Content(schema = @Schema(implementation = GeographicRestrictionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public GeographicRestrictionResponse addGeographicRestriction(
            @PathVariable UUID cardId,
            @Valid @RequestBody GeographicRestrictionRequest request
    ) {
        return restrictionService.addGeographicRestriction(cardId, request);
    }

    @GetMapping("/geographic")
    @Operation(
            summary = "Get geographic restrictions",
            description = "Retrieves all geographic restrictions for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restrictions retrieved"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public List<GeographicRestrictionResponse> getGeographicRestrictions(@PathVariable UUID cardId) {
        return restrictionService.getGeographicRestrictions(cardId);
    }

    @DeleteMapping("/geographic/{countryCode}")
    @Operation(
            summary = "Remove geographic restriction",
            description = "Removes a country restriction from a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Restriction removed"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public void removeGeographicRestriction(
            @PathVariable UUID cardId,
            @PathVariable String countryCode
    ) {
        restrictionService.removeGeographicRestriction(cardId, countryCode);
    }
}

