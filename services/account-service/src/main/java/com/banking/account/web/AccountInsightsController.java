package com.banking.account.web;

import com.banking.account.service.AccountInsightsService;
import com.banking.account.web.dto.AccountSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}")
@Tag(name = "Account Insights", description = "Summary and insight endpoints for accounts")
public class AccountInsightsController {

    private final AccountInsightsService accountInsightsService;

    public AccountInsightsController(AccountInsightsService accountInsightsService) {
        this.accountInsightsService = accountInsightsService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get account summary",
            description = "Returns a compact summary including balance, recent transactions and savings goals for an account."
    )
    @ApiResponse(responseCode = "200", description = "Summary returned",
            content = @Content(schema = @Schema(implementation = AccountSummaryResponse.class)))
    @PreAuthorize("@securityToggle.isDisabled() or hasAuthority('accounts.read')")
    public AccountSummaryResponse getAccountSummary(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID accountId
    ) {
        return accountInsightsService.getAccountSummary(accountId);
    }
}


