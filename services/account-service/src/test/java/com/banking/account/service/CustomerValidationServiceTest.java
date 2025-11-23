package com.banking.account.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerValidationServiceTest {

    @Mock
    private CustomerValidationService customerValidationService;

    @BeforeEach
    void setUp() {
        customerValidationService = new CustomerValidationService();
    }

    @Test
    void validateCustomerExistsDoesNotThrow() {
        UUID customerId = UUID.randomUUID();

        assertThatCode(() -> customerValidationService.validateCustomerExists(customerId))
                .doesNotThrowAnyException();
    }
}

