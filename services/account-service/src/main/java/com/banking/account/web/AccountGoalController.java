package com.banking.account.web;

import com.banking.account.service.AccountGoalService;
import com.banking.account.web.dto.AccountGoalResponse;
import com.banking.account.web.dto.CreateAccountGoalRequest;
import com.banking.account.web.dto.GoalContributionRequest;
import com.banking.account.web.dto.PageResponse;
import com.banking.account.web.dto.UpdateAccountGoalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/accounts/{accountId}/goals")
@Tag(name = "Account Goals", description = "Savings goal operations")
public class AccountGoalController {

    private final AccountGoalService accountGoalService;

    public AccountGoalController(AccountGoalService accountGoalService) {
        this.accountGoalService = accountGoalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create savings goal", description = "Creates a new savings goal and optionally enables auto-sweep.")
    @ApiResponse(responseCode = "201", description = "Goal created",
            content = @Content(schema = @Schema(implementation = AccountGoalResponse.class)))
    public AccountGoalResponse createGoal(
            @Parameter(description = "Account identifier", required = true) @PathVariable UUID accountId,
            @Valid @RequestBody CreateAccountGoalRequest request) {
        return accountGoalService.createGoal(accountId, request);
    }

    @GetMapping
    @Operation(summary = "List goals", description = "Returns a paginated list of goals for an account.")
    public PageResponse<AccountGoalResponse> listGoals(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accountGoalService.listGoals(accountId, page, size);
    }

    @GetMapping("/{goalId}")
    @Operation(summary = "Get goal", description = "Fetches a goal by id.")
    public AccountGoalResponse getGoal(
            @PathVariable UUID accountId,
            @PathVariable UUID goalId) {
        return accountGoalService.getGoal(accountId, goalId);
    }

    @PutMapping("/{goalId}")
    @Operation(summary = "Update goal", description = "Updates goal details, status, or auto-sweep configuration.")
    public AccountGoalResponse updateGoal(
            @PathVariable UUID accountId,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateAccountGoalRequest request) {
        return accountGoalService.updateGoal(accountId, goalId, request);
    }

    @PostMapping("/{goalId}/contributions")
    @Operation(summary = "Contribute to goal", description = "Records a manual contribution and debits the account balance.")
    public AccountGoalResponse contribute(
            @PathVariable UUID accountId,
            @PathVariable UUID goalId,
            @Valid @RequestBody GoalContributionRequest request) {
        return accountGoalService.contributeToGoal(accountId, goalId, request);
    }
}

