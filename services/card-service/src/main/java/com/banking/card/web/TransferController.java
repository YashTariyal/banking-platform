package com.banking.card.web;

import com.banking.card.service.TransferService;
import com.banking.card.web.dto.CardTransferRequest;
import com.banking.card.web.dto.CardTransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/{fromCardId}/transfers")
@Tag(name = "Card Transfers", description = "Card-to-card transfer operations")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Transfer between cards",
            description = "Transfers funds from one card to another card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer created",
                    content = @Content(schema = @Schema(implementation = CardTransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public CardTransferResponse transfer(
            @PathVariable UUID fromCardId,
            @Valid @RequestBody CardTransferRequest request
    ) {
        return transferService.transferBetweenCards(fromCardId, request);
    }
}

