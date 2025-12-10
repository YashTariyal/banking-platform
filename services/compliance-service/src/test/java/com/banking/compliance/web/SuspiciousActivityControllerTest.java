package com.banking.compliance.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.compliance.domain.Severity;
import com.banking.compliance.domain.SuspiciousActivity;
import com.banking.compliance.domain.SuspiciousActivityStatus;
import com.banking.compliance.domain.SuspiciousActivityType;
import com.banking.compliance.service.ComplianceService;
import com.banking.compliance.web.dto.SuspiciousActivityResponse;
import com.banking.compliance.web.dto.UpdateSuspiciousActivityRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SuspiciousActivityController.class)
class SuspiciousActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplianceService complianceService;

    @MockBean
    private ComplianceMapper complianceMapper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void getSuspiciousActivity_WhenExists_ReturnsActivity() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setId(id);
        SuspiciousActivityResponse response = createSuspiciousActivityResponse(activity);

        when(complianceService.getSuspiciousActivity(id)).thenReturn(activity);
        when(complianceMapper.toResponse(activity)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/suspicious-activities/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value(activity.getStatus().name()))
                .andExpect(jsonPath("$.severity").value(activity.getSeverity().name()));
    }

    @Test
    void listSuspiciousActivities_ReturnsPaginatedResults() throws Exception {
        // Given
        SuspiciousActivity activity = createSuspiciousActivity();
        Page<SuspiciousActivity> page = new PageImpl<>(List.of(activity));
        SuspiciousActivityResponse response = createSuspiciousActivityResponse(activity);

        when(complianceService.getSuspiciousActivities(any())).thenReturn(page);
        when(complianceMapper.toResponse(activity)).thenReturn(response);
        when(complianceMapper.toPageResponse(any(org.springframework.data.domain.Page.class))).thenAnswer(invocation -> {
            org.springframework.data.domain.Page<?> p = invocation.getArgument(0);
            return new com.banking.compliance.web.dto.PageResponse<>(
                    p.getContent(),
                    p.getNumber(),
                    p.getSize(),
                    p.getTotalElements(),
                    p.getTotalPages(),
                    p.hasNext(),
                    p.hasPrevious()
            );
        });

        // When/Then
        mockMvc.perform(get("/api/compliance/suspicious-activities")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listSuspiciousActivities_WithCustomerFilter_ReturnsFilteredResults() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setCustomerId(customerId);
        Page<SuspiciousActivity> page = new PageImpl<>(List.of(activity));
        SuspiciousActivityResponse response = createSuspiciousActivityResponse(activity);

        when(complianceService.getSuspiciousActivitiesByCustomer(eq(customerId), any()))
                .thenReturn(page);
        when(complianceMapper.toResponse(activity)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/suspicious-activities")
                        .param("customerId", customerId.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void listSuspiciousActivities_WithStatusFilter_ReturnsFilteredResults() throws Exception {
        // Given
        SuspiciousActivityStatus status = SuspiciousActivityStatus.OPEN;
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setStatus(status);
        Page<SuspiciousActivity> page = new PageImpl<>(List.of(activity));
        SuspiciousActivityResponse response = createSuspiciousActivityResponse(activity);

        when(complianceService.getSuspiciousActivitiesByStatus(eq(status), any()))
                .thenReturn(page);
        when(complianceMapper.toResponse(activity)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/suspicious-activities")
                        .param("status", status.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSuspiciousActivity_UpdatesStatusAndNotes() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        UUID investigatorId = UUID.randomUUID();
        SuspiciousActivity activity = createSuspiciousActivity();
        activity.setId(id);
        activity.setStatus(SuspiciousActivityStatus.UNDER_INVESTIGATION);
        activity.setInvestigatorId(investigatorId);
        activity.setInvestigationNotes("Investigation in progress");

        UpdateSuspiciousActivityRequest request = new UpdateSuspiciousActivityRequest(
                SuspiciousActivityStatus.UNDER_INVESTIGATION,
                investigatorId,
                "Investigation in progress"
        );

        SuspiciousActivityResponse response = createSuspiciousActivityResponse(activity);

        when(complianceService.updateSuspiciousActivityStatus(
                eq(id),
                eq(SuspiciousActivityStatus.UNDER_INVESTIGATION),
                eq(investigatorId),
                eq("Investigation in progress")))
                .thenReturn(activity);
        when(complianceMapper.toResponse(activity)).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/api/compliance/suspicious-activities/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(SuspiciousActivityStatus.UNDER_INVESTIGATION.name()))
                .andExpect(jsonPath("$.investigationNotes").value("Investigation in progress"));

        verify(complianceService).updateSuspiciousActivityStatus(
                id,
                SuspiciousActivityStatus.UNDER_INVESTIGATION,
                investigatorId,
                "Investigation in progress"
        );
    }

    private SuspiciousActivity createSuspiciousActivity() {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setId(UUID.randomUUID());
        activity.setCustomerId(UUID.randomUUID());
        activity.setAccountId(UUID.randomUUID());
        activity.setActivityType(SuspiciousActivityType.UNUSUAL_PATTERN);
        activity.setSeverity(Severity.MEDIUM);
        activity.setStatus(SuspiciousActivityStatus.OPEN);
        activity.setAmount(new BigDecimal("5000.00"));
        activity.setCurrency("USD");
        activity.setDescription("Test suspicious activity");
        activity.setRiskScore(60);
        activity.setCreatedAt(Instant.now());
        activity.setUpdatedAt(Instant.now());
        return activity;
    }

    private SuspiciousActivityResponse createSuspiciousActivityResponse(SuspiciousActivity activity) {
        return new SuspiciousActivityResponse(
                activity.getId(),
                activity.getCustomerId(),
                activity.getAccountId(),
                activity.getTransactionId(),
                activity.getActivityType(),
                activity.getSeverity(),
                activity.getStatus(),
                activity.getAmount(),
                activity.getCurrency(),
                activity.getDescription(),
                activity.getRiskScore(),
                activity.getComplianceRecordId(),
                activity.getInvestigatorId(),
                activity.getInvestigationNotes(),
                activity.getReportedAt(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}

