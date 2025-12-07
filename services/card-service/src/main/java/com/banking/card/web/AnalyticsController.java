package com.banking.card.web;

import com.banking.card.service.AnalyticsService;
import com.banking.card.web.dto.CardAnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/analytics")
@Tag(name = "Card Analytics", description = "Card usage analytics and reporting operations")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(
            summary = "Get card analytics",
            description = "Retrieves analytics and usage statistics for a card including transaction counts, amounts, and patterns."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics retrieved",
                    content = @Content(schema = @Schema(implementation = CardAnalyticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public CardAnalyticsResponse getAnalytics(@PathVariable UUID cardId) {
        return analyticsService.getCardAnalytics(cardId);
    }

    @PutMapping("/refresh")
    @Operation(
            summary = "Refresh analytics",
            description = "Recalculates and refreshes the analytics data for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics refreshed",
                    content = @Content(schema = @Schema(implementation = CardAnalyticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardAnalyticsResponse refreshAnalytics(@PathVariable UUID cardId) {
        return analyticsService.refreshAnalytics(cardId);
    }
}

