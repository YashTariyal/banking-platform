package com.banking.card.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.card.config.SecurityConfig;
import com.banking.card.config.SecurityToggle;
import com.banking.card.service.CardService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, SecurityToggle.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "card.security.enabled=false")
class SecurityDisabledWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @BeforeEach
    void setUp() {
        // For list endpoint, we don’t care about payload here – just that auth is not required.
        com.banking.card.web.dto.CardResponse dummy = new com.banking.card.web.dto.CardResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "**** **** **** 1234",
                com.banking.card.domain.CardStatus.ACTIVE,
                com.banking.card.domain.CardType.DEBIT,
                "USD",
                java.math.BigDecimal.TEN,
                java.time.Instant.now(),
                java.time.Instant.now(),
                null, null, null, false, false, null, false, null, null,
                java.time.LocalDate.now().plusYears(3), java.time.Instant.now(), null, null, false,
                null, false, null, null, null, null, null, null, 0, null, true
        );
        com.banking.card.web.dto.PageResponse<com.banking.card.web.dto.CardResponse> page =
                new com.banking.card.web.dto.PageResponse<>(Collections.singletonList(dummy), 1, 1, 0, 20);
        org.mockito.Mockito.when(cardService.listCards(null, 0, 20)).thenReturn(page);
    }

    @Test
    void whenSecurityDisabled_cardsApiIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/cards?page=0&size=20"))
                .andExpect(status().isOk());
    }
}


