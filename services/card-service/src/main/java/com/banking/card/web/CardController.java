package com.banking.card.web;

import com.banking.card.service.CardService;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.CreateCardRequest;
import com.banking.card.web.dto.PageResponse;
import com.banking.card.web.dto.UpdateCardLimitRequest;
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
}


