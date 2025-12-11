package com.banking.support.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.support.domain.CasePriority;
import com.banking.support.domain.CaseStatus;
import com.banking.support.domain.CaseType;
import com.banking.support.domain.SupportCase;
import com.banking.support.service.SupportCaseService;
import com.banking.support.web.dto.CreateCaseRequest;
import com.banking.support.web.dto.PageResponse;
import com.banking.support.web.dto.ResolveCaseRequest;
import com.banking.support.web.dto.SupportCaseResponse;
import com.banking.support.web.dto.UpdateCaseStatusRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(SupportCaseController.class)
class SupportCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportCaseService caseService;

    @MockBean
    private SupportMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID caseId;
    private UUID customerId;
    private UUID createdBy;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
    }

    @Test
    void createCase_validRequest_returnsCreated() throws Exception {
        CreateCaseRequest request = new CreateCaseRequest(
                CaseType.ACCOUNT_INQUIRY,
                CasePriority.HIGH,
                customerId,
                null,
                "Test Case",
                "Test description",
                null
        );

        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setCaseNumber("CASE1234567890");
        supportCase.setCaseType(CaseType.ACCOUNT_INQUIRY);

        SupportCaseResponse response = new SupportCaseResponse(
                caseId,
                "CASE1234567890",
                CaseType.ACCOUNT_INQUIRY,
                CasePriority.HIGH,
                CaseStatus.OPEN,
                customerId,
                null,
                "Test Case",
                "Test description",
                null,
                createdBy,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(caseService.createCase(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(supportCase);
        when(mapper.toResponse(supportCase)).thenReturn(response);

        mockMvc.perform(post("/api/support/cases")
                        .param("createdBy", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(caseId.toString()))
                .andExpect(jsonPath("$.caseType").value("ACCOUNT_INQUIRY"));
    }

    @Test
    void getCase_exists_returnsOk() throws Exception {
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setCaseNumber("CASE1234567890");

        SupportCaseResponse response = new SupportCaseResponse(
                caseId,
                "CASE1234567890",
                CaseType.ACCOUNT_INQUIRY,
                CasePriority.HIGH,
                CaseStatus.OPEN,
                customerId,
                null,
                "Test Case",
                null,
                null,
                createdBy,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(caseService.getCase(caseId)).thenReturn(supportCase);
        when(mapper.toResponse(supportCase)).thenReturn(response);

        mockMvc.perform(get("/api/support/cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId.toString()));
    }

    @Test
    void getCases_byStatus_returnsPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        SupportCase supportCase = new SupportCase();
        supportCase.setId(caseId);
        supportCase.setStatus(CaseStatus.OPEN);

        SupportCaseResponse response = new SupportCaseResponse(
                caseId,
                "CASE1234567890",
                CaseType.ACCOUNT_INQUIRY,
                CasePriority.HIGH,
                CaseStatus.OPEN,
                customerId,
                null,
                "Test Case",
                null,
                null,
                createdBy,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        Page<SupportCase> casePage = new PageImpl<>(List.of(supportCase), pageable, 1);
        when(caseService.getCasesByStatus(any(CaseStatus.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(casePage);
        when(mapper.toResponse(supportCase)).thenReturn(response);
        when(mapper.toPageResponse(any(Page.class)))
                .thenAnswer(invocation -> {
                    Page<?> page = invocation.getArgument(0);
                    return new PageResponse<>(
                            page.getContent(),
                            page.getNumber(),
                            page.getSize(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    );
                });

        mockMvc.perform(get("/api/support/cases")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(caseId.toString()));
    }
}

