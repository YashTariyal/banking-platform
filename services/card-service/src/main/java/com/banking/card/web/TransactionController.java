package com.banking.card.web;

import com.banking.card.service.TransactionService;
import com.banking.card.web.dto.CardTransactionResponse;
import com.banking.card.web.dto.CreateTransactionRequest;
import com.banking.card.web.dto.PageResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{cardId}/transactions")
@Tag(name = "Card Transactions", description = "Card transaction history operations")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a transaction",
            description = "Records a new transaction for a card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                    content = @Content(schema = @Schema(implementation = CardTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardTransactionResponse createTransaction(
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        return transactionService.createTransaction(cardId, request);
    }

    @GetMapping
    @Operation(
            summary = "Get transaction history",
            description = "Retrieves the transaction history for a card with pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.read')")
    public PageResponse<CardTransactionResponse> getTransactionHistory(
            @PathVariable UUID cardId,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        return transactionService.getTransactionHistory(cardId, page, size);
    }
}

