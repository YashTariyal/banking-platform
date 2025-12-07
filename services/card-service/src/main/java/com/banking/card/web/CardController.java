package com.banking.card.web;

import com.banking.card.service.CardService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Card management operations")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Issue a new card",
            description = "Issues a new card for a customer with an initial spending limit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card issued",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse issueCard(@Valid @RequestBody CreateCardRequest request) {
        return cardService.issueCard(request);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get card by ID",
            description = "Retrieves a card by its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public CardResponse getCard(@PathVariable UUID id) {
        return cardService.getCard(id);
    }

    @GetMapping
    @Operation(
            summary = "List cards",
            description = "Lists cards, optionally filtered by customer ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards listed",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public PageResponse<CardResponse> listCards(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return cardService.listCards(customerId, page, size);
    }

    @PutMapping("/{id}/activate")
    @Operation(
            summary = "Activate a card",
            description = "Moves a card from PENDING_ACTIVATION to ACTIVE if allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card activated",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid card state"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse activate(@PathVariable UUID id) {
        return cardService.activateCard(id);
    }

    @PutMapping("/{id}/block")
    @Operation(
            summary = "Block a card",
            description = "Blocks a card, preventing further usage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid card state"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse block(@PathVariable UUID id) {
        return cardService.blockCard(id);
    }

    @PutMapping("/{id}/limit")
    @Operation(
            summary = "Update card spending limit",
            description = "Updates the spending limit of a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limit updated",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse updateLimit(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCardLimitRequest request
    ) {
        return cardService.updateLimit(id, request);
    }

    @PutMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a card",
            description = "Cancels a card with a required reason. Once cancelled, the card cannot be reactivated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card cancelled",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or card already cancelled"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelCardRequest request
    ) {
        return cardService.cancelCard(id, request);
    }

    // Transaction Limits
    @PutMapping("/{id}/transaction-limits")
    @Operation(
            summary = "Update transaction limits",
            description = "Updates the daily and monthly transaction limits for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction limits updated",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse updateTransactionLimits(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionLimitsRequest request
    ) {
        return cardService.updateTransactionLimits(id, request);
    }

    // PIN Management
    @PutMapping("/{id}/pin")
    @Operation(
            summary = "Set PIN",
            description = "Sets a PIN for a card. PIN must be 4-6 digits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN set successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or card not active"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse setPin(
            @PathVariable UUID id,
            @Valid @RequestBody SetPinRequest request
    ) {
        return cardService.setPin(id, request);
    }

    @PutMapping("/{id}/pin/change")
    @Operation(
            summary = "Change PIN",
            description = "Changes the PIN for a card. Requires current PIN verification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN changed successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or incorrect current PIN"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse changePin(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePinRequest request
    ) {
        return cardService.changePin(id, request);
    }

    @PutMapping("/{id}/pin/reset-attempts")
    @Operation(
            summary = "Reset PIN attempts",
            description = "Resets the PIN attempt counter and unlocks the PIN if locked."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PIN attempts reset",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse resetPinAttempts(@PathVariable UUID id) {
        return cardService.resetPinAttempts(id);
    }

    // Freeze/Unfreeze
    @PutMapping("/{id}/freeze")
    @Operation(
            summary = "Freeze a card",
            description = "Freezes a card, temporarily preventing transactions. Card can be unfrozen later."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card frozen",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or card already frozen"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse freeze(
            @PathVariable UUID id,
            @Valid @RequestBody FreezeCardRequest request
    ) {
        return cardService.freezeCard(id, request);
    }

    @PutMapping("/{id}/unfreeze")
    @Operation(
            summary = "Unfreeze a card",
            description = "Unfreezes a previously frozen card, allowing transactions to resume."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card unfrozen",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Card is not frozen"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse unfreeze(@PathVariable UUID id) {
        return cardService.unfreezeCard(id);
    }

    // Card Replacement
    @PostMapping("/{id}/replace")
    @Operation(
            summary = "Replace a card",
            description = "Creates a new card to replace an existing one. The old card will be cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card replaced",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse replace(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceCardRequest request
    ) {
        return cardService.replaceCard(id, request);
    }

    // Account Linking
    @PutMapping("/{id}/account-link")
    @Operation(
            summary = "Link card to account",
            description = "Links a card to a specific account for transaction processing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account linked",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse linkAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountLinkRequest request
    ) {
        return cardService.updateAccountLink(id, request);
    }

    @PutMapping("/{id}/account-link/unlink")
    @Operation(
            summary = "Unlink card from account",
            description = "Removes the account link from a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account unlinked",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse unlinkAccount(@PathVariable UUID id) {
        return cardService.unlinkAccount(id);
    }

    // CVV Management
    @PutMapping("/{id}/cvv/rotate")
    @Operation(
            summary = "Rotate CVV",
            description = "Generates a new CVV for the card. The old CVV becomes invalid."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CVV rotated",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Card must be active"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse rotateCvv(@PathVariable UUID id) {
        return cardService.rotateCvv(id);
    }

    // ATM Withdrawal Limits
    @PutMapping("/{id}/atm-limits")
    @Operation(
            summary = "Update ATM withdrawal limits",
            description = "Updates the daily and monthly ATM withdrawal limits for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ATM limits updated",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse updateAtmLimits(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAtmLimitsRequest request
    ) {
        return cardService.updateAtmLimits(id, request);
    }

    // Card Renewal
    @PostMapping("/{id}/renew")
    @Operation(
            summary = "Renew a card",
            description = "Creates a new card with extended expiration date. The old card will be cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Card renewed",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or card already cancelled"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse renew(@PathVariable UUID id) {
        return cardService.renewCard(id);
    }

    // Contactless Payment Controls
    @PutMapping("/{id}/contactless/enable")
    @Operation(
            summary = "Enable contactless payments",
            description = "Enables contactless payment functionality for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contactless enabled",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse enableContactless(@PathVariable UUID id) {
        return cardService.enableContactless(id);
    }

    @PutMapping("/{id}/contactless/disable")
    @Operation(
            summary = "Disable contactless payments",
            description = "Disables contactless payment functionality for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contactless disabled",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardResponse disableContactless(@PathVariable UUID id) {
        return cardService.disableContactless(id);
    }
}


