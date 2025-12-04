package com.banking.card.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.card.config.JwtAuthConverter;
import com.banking.card.config.SecurityConfig;
import com.banking.card.config.SecurityToggle;
import com.banking.card.service.CardService;
import com.banking.card.web.dto.CardResponse;
import com.banking.card.web.dto.PageResponse;
import com.banking.card.domain.CardStatus;
import com.banking.card.domain.CardType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityToggle.class, JwtAuthConverter.class})
@TestPropertySource(properties = {
        "card.security.enabled=true"
})
class SecurityEnabledWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CardService cardService;

    @BeforeEach
    void setUp() {
        CardResponse dummy = new CardResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "**** **** **** 1234",
                CardStatus.ACTIVE,
                CardType.DEBIT,
                "USD",
                BigDecimal.TEN,
                Instant.now(),
                Instant.now()
        );
        PageResponse<CardResponse> page = new PageResponse<>(
                Collections.singletonList(dummy), 1, 1, 0, 20);
        when(cardService.listCards(isNull(), anyInt(), anyInt())).thenReturn(page);
    }

    @Test
    void whenSecurityEnabled_requestsWithoutTokenReturn401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenSecurityEnabled_requestsWithValidJwtSucceed() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("user-123")
                                .claim("scope", "cards.read")
                        ).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("cards.read"))))
                .andExpect(status().isOk());
    }
}


