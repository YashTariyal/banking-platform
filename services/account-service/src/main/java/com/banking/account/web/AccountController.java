package com.banking.account.web;

import com.banking.account.service.AccountService;
import com.banking.account.web.dto.AccountResponse;
import com.banking.account.web.dto.AccountTransactionRequest;
import com.banking.account.web.dto.BalanceResponse;
import com.banking.account.web.dto.BulkAccountResponse;
import com.banking.account.web.dto.BulkCreateAccountRequest;
import com.banking.account.web.dto.BulkTransactionRequest;
import com.banking.account.web.dto.BulkUpdateStatusRequest;
import com.banking.account.web.dto.CreateAccountRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.TransactionHistoryResponse;
import com.banking.account.web.dto.UpdateAccountRequest;
import com.banking.account.web.dto.UpdateAccountStatusRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new account",
            description = "Creates a new account for a customer with an initial deposit. The account number is auto-generated and the account status is set to ACTIVE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get account by ID",
            description = "Retrieves detailed information about a specific account by its unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse getAccount(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id) {
        return accountService.getAccount(id);
    }

    @GetMapping
    @Operation(
            summary = "List accounts",
            description = "Retrieves a paginated list of accounts. Optionally filter by customer ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public PageResponse<AccountResponse> listAccounts(
            @Parameter(description = "Filter by customer ID (optional)")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return accountService.listAccounts(customerId, page, size);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Update account status",
            description = "Updates the status of an account. Valid statuses are: ACTIVE, SUSPENDED, CLOSED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public AccountResponse updateStatus(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return accountService.updateStatus(id, request);
    }

    @PostMapping("/{id}/transactions")
    @Operation(
            summary = "Apply transaction to account",
            description = """
                    Applies a credit or debit transaction to an account.
                    Transactions are idempotent - duplicate requests with the same referenceId are ignored.
                    Debit transactions require sufficient balance. Transactions are blocked on CLOSED or SUSPENDED accounts.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction applied successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transaction (insufficient balance, invalid account status, etc.)"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict")
    })
    @Tag(name = "Transactions")
    public AccountResponse applyTransaction(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody AccountTransactionRequest request) {
        return accountService.applyTransaction(id, request);
    }

    @GetMapping("/{id}/balance")
    @Operation(
            summary = "Get account balance",
            description = "Retrieves the current balance and currency information for an account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @Tag(name = "Balance")
    public BalanceResponse getBalance(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/{id}/transactions")
    @Operation(
            summary = "Get transaction history",
            description = "Retrieves a paginated list of transactions for an account, ordered by creation date (most recent first)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @Tag(name = "Transactions")
    public PageResponse<TransactionHistoryResponse> getTransactionHistory(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return accountService.getTransactionHistory(id, page, size);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update account details",
            description = "Updates account type and/or currency. Both fields are optional - only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public AccountResponse updateAccount(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete account",
            description = "Permanently deletes an account from the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public void deleteAccount(
            @Parameter(description = "Account unique identifier", required = true)
            @PathVariable UUID id) {
        accountService.deleteAccount(id);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Bulk create accounts",
            description = "Creates multiple accounts for a customer in a single request. Maximum 100 accounts per request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bulk account creation completed",
                    content = @Content(schema = @Schema(implementation = BulkAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @Tag(name = "Bulk Operations")
    public BulkAccountResponse bulkCreateAccounts(@Valid @RequestBody BulkCreateAccountRequest request) {
        return accountService.bulkCreateAccounts(request);
    }

    @PutMapping("/bulk/status")
    @Operation(
            summary = "Bulk update account statuses",
            description = "Updates the status of multiple accounts in a single request. Maximum 100 accounts per request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk status update completed",
                    content = @Content(schema = @Schema(implementation = BulkAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @Tag(name = "Bulk Operations")
    public BulkAccountResponse bulkUpdateStatus(@Valid @RequestBody BulkUpdateStatusRequest request) {
        return accountService.bulkUpdateStatus(request);
    }

    @PostMapping("/bulk/transactions")
    @Operation(
            summary = "Bulk process transactions",
            description = "Processes multiple transactions in a single request. Maximum 100 transactions per request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk transaction processing completed",
                    content = @Content(schema = @Schema(implementation = BulkAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @Tag(name = "Bulk Operations")
    public BulkAccountResponse bulkProcessTransactions(@Valid @RequestBody BulkTransactionRequest request) {
        return accountService.bulkProcessTransactions(request);
    }
}

