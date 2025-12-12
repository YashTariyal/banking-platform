package com.banking.risk.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.risk.domain.RiskAssessment;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.domain.RiskType;
import com.banking.risk.service.RiskAssessmentService;
import com.banking.risk.web.dto.AssessRiskRequest;
import com.banking.risk.web.dto.PageResponse;
import com.banking.risk.web.dto.RiskAssessmentResponse;
import com.banking.risk.config.RequestLoggingFilter;
import com.banking.risk.config.PiiMaskingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import io.micrometer.core.instrument.MeterRegistry;

@WebMvcTest(RiskAssessmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RequestLoggingFilter.class, PiiMaskingFilter.class})
@TestPropertySource(properties = {
        "risk.security.enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
class RiskAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskAssessmentService assessmentService;

    @MockBean
    private RiskMapper mapper;

    @MockBean
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID assessmentId;
    private UUID entityId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        assessmentId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void assessRisk_validRequest_returnsCreated() throws Exception {
        AssessRiskRequest request = new AssessRiskRequest(
                RiskType.TRANSACTION,
                entityId,
                customerId,
                null,
                new BigDecimal("1000"),
                "USD",
                "Test transaction"
        );

        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(assessmentId);
        assessment.setRiskType(RiskType.TRANSACTION);
        assessment.setEntityId(entityId);
        assessment.setRiskLevel(RiskLevel.LOW);
        assessment.setRiskScore(20);
        assessment.setAssessedAt(Instant.now());

        RiskAssessmentResponse response = new RiskAssessmentResponse(
                assessmentId,
                RiskType.TRANSACTION,
                entityId,
                RiskLevel.LOW,
                20,
                customerId,
                null,
                new BigDecimal("1000"),
                "USD",
                "Low risk",
                "Test transaction",
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        when(assessmentService.assessRisk(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(assessment);
        when(mapper.toResponse(assessment)).thenReturn(response);

        mockMvc.perform(post("/api/risk/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.riskType").value("TRANSACTION"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void getAssessment_exists_returnsOk() throws Exception {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(assessmentId);
        assessment.setRiskType(RiskType.TRANSACTION);

        RiskAssessmentResponse response = new RiskAssessmentResponse(
                assessmentId,
                RiskType.TRANSACTION,
                entityId,
                RiskLevel.LOW,
                20,
                customerId,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        when(assessmentService.getAssessment(assessmentId)).thenReturn(assessment);
        when(mapper.toResponse(assessment)).thenReturn(response);

        mockMvc.perform(get("/api/risk/assessments/{id}", assessmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()));
    }

    @Test
    void getAssessments_byRiskType_returnsPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(assessmentId);
        assessment.setRiskType(RiskType.TRANSACTION);

        RiskAssessmentResponse response = new RiskAssessmentResponse(
                assessmentId,
                RiskType.TRANSACTION,
                entityId,
                RiskLevel.LOW,
                20,
                customerId,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Page<RiskAssessment> assessmentPage = new PageImpl<>(List.of(assessment), pageable, 1);
        when(assessmentService.getAssessmentsByType(any(RiskType.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(assessmentPage);
        when(mapper.toResponse(assessment)).thenReturn(response);
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

        mockMvc.perform(get("/api/risk/assessments")
                        .param("riskType", "TRANSACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(assessmentId.toString()));
    }
}

