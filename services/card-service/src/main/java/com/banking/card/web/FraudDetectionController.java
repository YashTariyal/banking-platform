package com.banking.card.web;

import com.banking.card.service.FraudDetectionService;
import com.banking.card.web.dto.FraudCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/fraud")
@Tag(name = "Fraud Detection", description = "Fraud detection and prevention operations")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/check")
    @Operation(
            summary = "Check for fraud",
            description = "Performs fraud detection checks including velocity checks, unusual patterns, and location analysis."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fraud check result",
                    content = @Content(schema = @Schema(implementation = FraudCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public FraudCheckResponse checkForFraud(
            @PathVariable UUID cardId,
            @RequestParam @NotNull @Positive BigDecimal amount,
            @RequestParam(required = false) String merchantCountry
    ) {
        return fraudDetectionService.checkForFraud(cardId, amount, merchantCountry);
    }
}

