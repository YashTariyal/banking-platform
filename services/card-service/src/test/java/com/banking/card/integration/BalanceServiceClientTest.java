package com.banking.card.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.card.integration.StubBalanceServiceClient;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BalanceServiceClientTest {

    private BalanceServiceClient balanceServiceClient;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        balanceServiceClient = new StubBalanceServiceClient();
        accountId = UUID.randomUUID();
    }

    @Test
    void hasSufficientBalanceReturnsTrue() {
        boolean result = balanceServiceClient.hasSufficientBalance(
                accountId, BigDecimal.valueOf(1000), "USD");
        
        assertThat(result).isTrue();
    }

    @Test
    void getAvailableBalanceReturnsLargeAmount() {
        BigDecimal balance = balanceServiceClient.getAvailableBalance(accountId, "USD");
        
        assertThat(balance).isNotNull();
        assertThat(balance).isGreaterThan(BigDecimal.ZERO);
    }
}

