package com.banking.identity.web.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {
}

