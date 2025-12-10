package com.banking.identity.web.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String username,
        UUID customerId,
        String email,
        Boolean emailVerified
) {
}

