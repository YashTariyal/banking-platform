package com.banking.card.web;

import com.banking.card.service.AuthorizationService;
import com.banking.card.web.dto.AuthorizationRequestDto;
import com.banking.card.web.dto.AuthorizationResponse;
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
@RequestMapping("/api/cards/{cardId}/authorize")
@Tag(name = "Card Authorization", description = "Real-time transaction authorization operations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Operation(
            summary = "Authorize transaction",
            description = "Performs real-time authorization checks for a transaction including card status, limits, and restrictions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization result",
                    content = @Content(schema = @Schema(implementation = AuthorizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('cards.write')")
    public AuthorizationResponse authorize(
            @PathVariable UUID cardId,
            @Valid @RequestBody AuthorizationRequestDto request
    ) {
        return authorizationService.authorizeTransaction(cardId, request);
    }
}

