package com.banking.card.web;

import com.banking.card.service.BalanceVerificationService;
import com.banking.card.web.dto.BalanceVerificationRequest;
import com.banking.card.web.dto.BalanceVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/balance")
@Tag(name = "Balance Verification", description = "Balance verification operations")
public class BalanceVerificationController {

    private final BalanceVerificationService balanceVerificationService;

    public BalanceVerificationController(BalanceVerificationService balanceVerificationService) {
        this.balanceVerificationService = balanceVerificationService;
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify balance",
            description = "Verifies if the card has sufficient balance for a transaction. Integrates with account/ledger service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance verification result",
                    content = @Content(schema = @Schema(implementation = BalanceVerificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public BalanceVerificationResponse verifyBalance(
            @PathVariable UUID cardId,
            @Valid @RequestBody BalanceVerificationRequest request
    ) {
        return balanceVerificationService.verifyBalance(cardId, request);
    }
}

