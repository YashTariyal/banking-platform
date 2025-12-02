package com.banking.account.security;

import com.banking.account.domain.Account;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessEvaluator {

    /**
     * Enforce that the authenticated principal is allowed to access the given account.
     * <p>
     * Convention:
     * - Prefer claim "customer_id" (UUID string)
     * - Fallback to "customerId"
     */
    public void assertCanAccessAccount(Account account) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Missing authenticated principal for tenant evaluation");
        }

        UUID accountCustomerId = account.getCustomerId();
        if (accountCustomerId == null) {
            return;
        }

        Object claim = jwt.getClaims().getOrDefault("customer_id",
                jwt.getClaims().get("customerId"));
        if (claim == null) {
            throw new AccessDeniedException("JWT is missing customer identifier required for account access");
        }

        UUID tokenCustomerId;
        try {
            tokenCustomerId = UUID.fromString(claim.toString());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid customer identifier in JWT", ex);
        }

        if (!accountCustomerId.equals(tokenCustomerId)) {
            throw new AccessDeniedException("Access to account is not allowed for this customer");
        }
    }
}


