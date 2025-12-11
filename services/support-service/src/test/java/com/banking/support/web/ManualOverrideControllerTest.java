package com.banking.support.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.support.domain.ManualOverride;
import com.banking.support.domain.OverrideStatus;
import com.banking.support.domain.OverrideType;
import com.banking.support.service.ManualOverrideService;
import com.banking.support.web.dto.CreateOverrideRequest;
import com.banking.support.web.dto.ManualOverrideResponse;
import com.banking.support.web.dto.PageResponse;
import com.banking.support.web.dto.RejectOverrideRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManualOverrideController.class)
class ManualOverrideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManualOverrideService overrideService;

    @MockBean
    private SupportMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID overrideId;
    private UUID customerId;
    private UUID requestedBy;

    @BeforeEach
    void setUp() {
        overrideId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        requestedBy = UUID.randomUUID();
    }

    @Test
    void createOverride_validRequest_returnsOk() throws Exception {
        CreateOverrideRequest request = new CreateOverrideRequest(
                OverrideType.ACCOUNT_LIMIT,
                customerId,
                null,
                null,
                "Test reason",
                "10000",
                new BigDecimal("10000"),
                "USD",
                null
        );

        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setOverrideType(OverrideType.ACCOUNT_LIMIT);

        ManualOverrideResponse response = new ManualOverrideResponse(
                overrideId,
                OverrideType.ACCOUNT_LIMIT,
                OverrideStatus.PENDING,
                customerId,
                null,
                null,
                requestedBy,
                null,
                null,
                "Test reason",
                "10000",
                new BigDecimal("10000"),
                "USD",
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(overrideService.createOverride(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(override);
        when(mapper.toResponse(override)).thenReturn(response);

        mockMvc.perform(post("/api/support/overrides")
                        .param("requestedBy", requestedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overrideId.toString()))
                .andExpect(jsonPath("$.overrideType").value("ACCOUNT_LIMIT"));
    }

    @Test
    void approveOverride_validRequest_returnsOk() throws Exception {
        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.APPROVED);

        ManualOverrideResponse response = new ManualOverrideResponse(
                overrideId,
                OverrideType.ACCOUNT_LIMIT,
                OverrideStatus.APPROVED,
                customerId,
                null,
                null,
                requestedBy,
                UUID.randomUUID(),
                null,
                "Test reason",
                null,
                null,
                null,
                null,
                Instant.now(),
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(overrideService.approveOverride(any(UUID.class), any(UUID.class)))
                .thenReturn(override);
        when(mapper.toResponse(override)).thenReturn(response);

        mockMvc.perform(put("/api/support/overrides/{id}/approve", overrideId)
                        .param("approvedBy", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectOverride_validRequest_returnsOk() throws Exception {
        RejectOverrideRequest request = new RejectOverrideRequest("Rejected");

        ManualOverride override = new ManualOverride();
        override.setId(overrideId);
        override.setStatus(OverrideStatus.REJECTED);

        ManualOverrideResponse response = new ManualOverrideResponse(
                overrideId,
                OverrideType.ACCOUNT_LIMIT,
                OverrideStatus.REJECTED,
                customerId,
                null,
                null,
                requestedBy,
                null,
                UUID.randomUUID(),
                "Test reason",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "Rejected",
                Instant.now(),
                Instant.now()
        );

        when(overrideService.rejectOverride(any(UUID.class), any(String.class), any(UUID.class)))
                .thenReturn(override);
        when(mapper.toResponse(override)).thenReturn(response);

        mockMvc.perform(put("/api/support/overrides/{id}/reject", overrideId)
                        .param("rejectedBy", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Rejected"));
    }
}

