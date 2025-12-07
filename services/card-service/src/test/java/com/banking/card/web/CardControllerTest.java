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
import java.time.LocalDate;
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

    private CardResponse createTestCardResponse(UUID id, UUID customerId, CardStatus status) {
        return new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                status,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now(),
                null, // cancellationReason
                null, // dailyTransactionLimit
                null, // monthlyTransactionLimit
                false, // pinSet
                false, // pinLocked
                null, // pinLockedUntil
                false, // frozen
                null, // frozenAt
                null, // frozenReason
                LocalDate.now().plusYears(3), // expirationDate
                Instant.now(), // issuedAt
                null, // replacedByCardId
                null, // replacementReason
                false, // isReplacement
                null, // accountId
                false, // cvvSet
                null, // cvvGeneratedAt
                null, // cvvRotationDueDate
                null, // cardholderName
                null, // dailyAtmLimit
                null, // monthlyAtmLimit
                null, // renewedFromCardId
                0, // renewalCount
                null, // lastRenewedAt
                true // contactlessEnabled
        );
    }

    @Test
    void issueCardReturnsCreatedCard() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.PENDING_ACTIVATION);

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
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.getCard(id)).thenReturn(response);

        mockMvc.perform(get("/api/cards/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listCardsReturnsPagedResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse card = createTestCardResponse(id, customerId, CardStatus.ACTIVE);
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
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

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
                id, customerId, "**** **** **** 1234", CardStatus.ACTIVE, CardType.DEBIT, "USD",
                new BigDecimal("2000.00"), Instant.now(), Instant.now(), null,
                null, null, false, false, null, false, null, null,
                LocalDate.now().plusYears(3), Instant.now(), null, null, false,
                null, false, null, null, null, null, null, null, 0, null, true
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

    @Test
    void cancelCardReturnsCancelledStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id,
                customerId,
                "**** **** **** 1234",
                CardStatus.CANCELLED,
                CardType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                Instant.now(),
                Instant.now(),
                "Customer requested cancellation",
                null, null, false, false, null, false, null, null,
                LocalDate.now().plusYears(3), Instant.now(), null, null, false,
                null, false, null, null, null, null, null, null, 0, null, true
        );

        when(cardService.cancelCard(any(), any())).thenReturn(response);

        String body = """
                {
                  "reason": "Customer requested cancellation"
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Customer requested cancellation"));
    }

    // Transaction Limits Tests
    @Test
    void updateTransactionLimitsReturnsUpdatedLimits() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.updateTransactionLimits(any(), any())).thenReturn(response);

        String body = """
                {
                  "dailyLimit": 500.00,
                  "monthlyLimit": 5000.00
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/transaction-limits", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // PIN Management Tests
    @Test
    void setPinReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.setPin(any(), any())).thenReturn(response);

        String body = """
                {
                  "pin": "1234"
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/pin", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void changePinReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.changePin(any(), any())).thenReturn(response);

        String body = """
                {
                  "currentPin": "1234",
                  "newPin": "5678"
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/pin/change", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void resetPinAttemptsReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.resetPinAttempts(any())).thenReturn(response);

        mockMvc.perform(put("/api/cards/{id}/pin/reset-attempts", id))
                .andExpect(status().isOk());
    }

    // Freeze/Unfreeze Tests
    @Test
    void freezeCardReturnsFrozenStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                id, customerId, "**** **** **** 1234", CardStatus.ACTIVE, CardType.DEBIT, "USD",
                new BigDecimal("1000.00"), Instant.now(), Instant.now(), null,
                null, null, false, false, null,
                true, Instant.now(), "Suspicious activity",
                LocalDate.now().plusYears(3), Instant.now(), null, null, false,
                null, false, null, null, null, null, null, null, 0, null, true
        );

        when(cardService.freezeCard(any(), any())).thenReturn(response);

        String body = """
                {
                  "reason": "Suspicious activity"
                }
                """;

        mockMvc.perform(put("/api/cards/{id}/freeze", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frozen").value(true))
                .andExpect(jsonPath("$.frozenReason").value("Suspicious activity"));
    }

    @Test
    void unfreezeCardReturnsUnfrozenStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = createTestCardResponse(id, customerId, CardStatus.ACTIVE);

        when(cardService.unfreezeCard(any())).thenReturn(response);

        mockMvc.perform(put("/api/cards/{id}/unfreeze", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frozen").value(false));
    }

    // Card Replacement Tests
    @Test
    void replaceCardReturnsNewCard() throws Exception {
        UUID oldCardId = UUID.randomUUID();
        UUID newCardId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CardResponse response = new CardResponse(
                newCardId, customerId, "**** **** **** 5678", CardStatus.PENDING_ACTIVATION,
                CardType.DEBIT, "USD", new BigDecimal("1000.00"),
                Instant.now(), Instant.now(), null,
                null, null, false, false, null,
                false, null, null,
                LocalDate.now().plusYears(3), Instant.now(),
                null, "Lost card", true,
                null, false, null, null, null, null, null, null, 0, null, true
        );

        when(cardService.replaceCard(any(), any())).thenReturn(response);

        String body = """
                {
                  "reason": "Lost card"
                }
                """;

        mockMvc.perform(post("/api/cards/{id}/replace", oldCardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_ACTIVATION"))
                .andExpect(jsonPath("$.isReplacement").value(true))
                .andExpect(jsonPath("$.replacementReason").value("Lost card"));
    }
}


