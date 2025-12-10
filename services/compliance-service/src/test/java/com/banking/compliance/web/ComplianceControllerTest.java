package com.banking.compliance.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.compliance.domain.ComplianceRecord;
import com.banking.compliance.domain.ComplianceRecordType;
import com.banking.compliance.domain.ComplianceStatus;
import com.banking.compliance.service.ComplianceService;
import com.banking.compliance.web.dto.ComplianceRecordResponse;
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

@WebMvcTest(ComplianceController.class)
class ComplianceControllerTest {

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
    void getComplianceRecord_WhenExists_ReturnsRecord() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        record.setId(id);
        ComplianceRecordResponse response = createComplianceRecordResponse(record);

        when(complianceService.getComplianceRecord(id)).thenReturn(record);
        when(complianceMapper.toResponse(record)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/records/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.customerId").value(record.getCustomerId().toString()))
                .andExpect(jsonPath("$.status").value(record.getStatus().name()));
    }

    @Test
    void getComplianceRecordsByCustomer_ReturnsPaginatedResults() throws Exception {
        // Given
        UUID customerId = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        Page<ComplianceRecord> page = new PageImpl<>(List.of(record));
        ComplianceRecordResponse response = createComplianceRecordResponse(record);

        when(complianceService.getComplianceRecordsByCustomer(eq(customerId), any()))
                .thenReturn(page);
        when(complianceMapper.toResponse(record)).thenReturn(response);
        when(complianceMapper.toPageResponse(any(Page.class))).thenAnswer(invocation -> {
            Page<?> p = invocation.getArgument(0);
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
        mockMvc.perform(get("/api/compliance/records/customer/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void getComplianceRecordsByAccount_ReturnsPaginatedResults() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        record.setAccountId(accountId);
        Page<ComplianceRecord> page = new PageImpl<>(List.of(record));
        ComplianceRecordResponse response = createComplianceRecordResponse(record);

        when(complianceService.getComplianceRecordsByAccount(eq(accountId), any()))
                .thenReturn(page);
        when(complianceMapper.toResponse(record)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/records/account/{accountId}", accountId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void listComplianceRecords_WithStatusFilter_ReturnsFilteredResults() throws Exception {
        // Given
        ComplianceStatus status = ComplianceStatus.FLAGGED;
        ComplianceRecord record = createComplianceRecord();
        record.setStatus(status);
        Page<ComplianceRecord> page = new PageImpl<>(List.of(record));
        ComplianceRecordResponse response = createComplianceRecordResponse(record);

        when(complianceService.getComplianceRecordsByStatus(eq(status), any()))
                .thenReturn(page);
        when(complianceMapper.toResponse(record)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/compliance/records")
                        .param("status", status.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void updateComplianceRecordStatus_UpdatesStatus() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        ComplianceRecord record = createComplianceRecord();
        record.setId(id);
        record.setStatus(ComplianceStatus.FLAGGED);
        ComplianceRecordResponse response = createComplianceRecordResponse(record);

        ComplianceController.UpdateStatusRequest request =
                new ComplianceController.UpdateStatusRequest(ComplianceStatus.FLAGGED);

        when(complianceService.updateComplianceRecordStatus(id, ComplianceStatus.FLAGGED))
                .thenReturn(record);
        when(complianceMapper.toResponse(record)).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/api/compliance/records/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ComplianceStatus.FLAGGED.name()));

        verify(complianceService).updateComplianceRecordStatus(id, ComplianceStatus.FLAGGED);
    }

    private ComplianceRecord createComplianceRecord() {
        ComplianceRecord record = new ComplianceRecord();
        record.setId(UUID.randomUUID());
        record.setCustomerId(UUID.randomUUID());
        record.setAccountId(UUID.randomUUID());
        record.setRecordType(ComplianceRecordType.TRANSACTION_MONITORING);
        record.setStatus(ComplianceStatus.PENDING);
        record.setAmount(new BigDecimal("1000.00"));
        record.setCurrency("USD");
        record.setRiskScore(50);
        record.setFlags("TEST_FLAG");
        record.setSourceEventType("TRANSACTION_COMPLETED");
        record.setSourceTopic("transaction-events");
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return record;
    }

    private ComplianceRecordResponse createComplianceRecordResponse(ComplianceRecord record) {
        return new ComplianceRecordResponse(
                record.getId(),
                record.getCustomerId(),
                record.getAccountId(),
                record.getTransactionId(),
                record.getRecordType(),
                record.getStatus(),
                record.getAmount(),
                record.getCurrency(),
                record.getDescription(),
                record.getRiskScore(),
                record.getFlags(),
                record.getSourceEventType(),
                record.getSourceTopic(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}

