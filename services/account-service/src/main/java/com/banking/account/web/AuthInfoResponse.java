package com.banking.account.web;

import java.util.List;

public record AuthInfoResponse(
        String subject,
        String issuer,
        List<String> scopes,
        List<String> authorities
) {
}


