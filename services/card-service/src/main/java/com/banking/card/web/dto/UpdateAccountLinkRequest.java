package com.banking.card.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateAccountLinkRequest(
        @NotNull(message = "Account ID is required")
        UUID accountId
) {
}

