package com.banking.risk.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.risk.domain.AlertStatus;
import com.banking.risk.domain.RiskAlert;
import com.banking.risk.domain.RiskLevel;
import com.banking.risk.service.RiskAlertService;
import com.banking.risk.web.dto.PageResponse;
import com.banking.risk.web.dto.RiskAlertResponse;
import com.banking.risk.web.dto.UpdateAlertStatusRequest;
import com.banking.risk.config.RequestLoggingFilter;
import com.banking.risk.config.PiiMaskingFilter;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(RiskAlertController.class)
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
class RiskAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskAlertService alertService;

    @MockBean
    private RiskMapper mapper;

    @MockBean
    private MeterRegistry meterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID alertId;
    private UUID assessmentId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        alertId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void getAlert_exists_returnsOk() throws Exception {
        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.OPEN);

        RiskAlertResponse response = new RiskAlertResponse(
                alertId,
                assessmentId,
                AlertStatus.OPEN,
                RiskLevel.HIGH,
                75,
                customerId,
                null,
                "Test Alert",
                "Test description",
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(alertService.getAlert(alertId)).thenReturn(alert);
        when(mapper.toResponse(alert)).thenReturn(response);

        mockMvc.perform(get("/api/risk/alerts/{id}", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getAlerts_byStatus_returnsPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.OPEN);

        RiskAlertResponse response = new RiskAlertResponse(
                alertId,
                assessmentId,
                AlertStatus.OPEN,
                RiskLevel.HIGH,
                75,
                customerId,
                null,
                "Test Alert",
                "Test description",
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        Page<RiskAlert> alertPage = new PageImpl<>(List.of(alert), pageable, 1);
        when(alertService.getAlertsByStatus(any(AlertStatus.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(alertPage);
        when(mapper.toResponse(alert)).thenReturn(response);
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

        mockMvc.perform(get("/api/risk/alerts")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(alertId.toString()));
    }

    @Test
    void updateAlertStatus_validRequest_returnsOk() throws Exception {
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest(
                AlertStatus.RESOLVED,
                UUID.randomUUID(),
                "Resolved as false positive"
        );

        RiskAlert alert = new RiskAlert();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.RESOLVED);

        RiskAlertResponse response = new RiskAlertResponse(
                alertId,
                assessmentId,
                AlertStatus.RESOLVED,
                RiskLevel.HIGH,
                75,
                customerId,
                null,
                "Test Alert",
                "Test description",
                request.reviewedBy(),
                Instant.now(),
                request.resolutionNotes(),
                Instant.now(),
                Instant.now()
        );

        when(alertService.updateAlertStatus(any(), any(), any(), any()))
                .thenReturn(alert);
        when(mapper.toResponse(alert)).thenReturn(response);

        mockMvc.perform(put("/api/risk/alerts/{id}/status", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Resolved as false positive"));
    }
}

