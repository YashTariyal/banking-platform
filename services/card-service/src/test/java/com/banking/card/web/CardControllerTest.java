package com.banking.card.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import com.banking.card.service.CardService;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.banking.card.config.SecurityConfig;
import com.banking.card.config.SecurityToggle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {CardController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, SecurityToggle.class})
@TestPropertySource(properties = "card.security.enabled=false")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    void issueCardReturnsCreatedCard() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.PENDING_ACTIVATION,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now()
        );

        when(cardService.issueCard(any())).thenReturn(response);

        String body = """
                {
                  "customerId": "%s",
                  "type": "DEBIT",
                  "currency": "USD",
                  "spendingLimit": 1000.00
                }
                """.formatted(customerId);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    void getCardReturnsCard() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.ACTIVE,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now()
        );

        when(cardService.getCard(id)).thenReturn(response);

        mockMvc.perform(get("/api/cards/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listCardsReturnsPagedResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse card = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.ACTIVE,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now()
        );
        PageResponse<CardResponse> page = new PageResponse<>(List.of(card), 1, 1, 0, 20);

        when(cardService.listCards(null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/cards?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void activateCardReturnsUpdatedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.ACTIVE,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now()
        );

        when(cardService.activateCard(id)).thenReturn(response);

        mockMvc.perform(put("/api/cards/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateLimitReturnsUpdatedLimit() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.ACTIVE,
                CardType.DEBIT,
                "USD",
                new BigDecimal("2000.00"),
                Instant.now(),
                Instant.now()
        );

        when(cardService.updateLimit(any(), any())).thenReturn(response);

        String body = """
                {
                  "spendingLimit": 2000.00
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/limit", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spendingLimit").value(2000.00));
    }
}


