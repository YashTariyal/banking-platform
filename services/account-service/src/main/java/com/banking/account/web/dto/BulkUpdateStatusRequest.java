package com.banking.account.web.dto;

import com.banking.account.domain.AccountStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BulkUpdateStatusRequest(
        @NotEmpty
        @Size(min = 1, max = 100, message = "Must update between 1 and 100 accounts at a time")
        List<UUID> accountIds,
        @NotNull AccountStatus status
) {
}

