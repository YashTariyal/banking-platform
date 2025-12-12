package com.banking.loan.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.loan.domain.LoanApplication;
import com.banking.loan.domain.LoanStatus;
import com.banking.loan.service.LoanService;
import com.banking.loan.web.dto.LoanApplicationResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import io.micrometer.core.instrument.MeterRegistry;
import com.banking.loan.config.RequestLoggingFilter;
import com.banking.loan.config.PiiMaskingFilter;

@WebMvcTest(controllers = LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RequestLoggingFilter.class, PiiMaskingFilter.class})
@TestPropertySource(properties = {
        "loan.security.enabled=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none"
})
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private LoanMapper mapper;

    @MockBean
    private MeterRegistry meterRegistry;

    private LoanApplication loan;
    private LoanApplicationResponse response;

    @BeforeEach
    void setup() {
        loan = new LoanApplication();
        loan.setId(UUID.randomUUID());
        loan.setCustomerId(UUID.randomUUID());
        loan.setPrincipal(new BigDecimal("1000.00"));
        loan.setAnnualInterestRate(new BigDecimal("12.0"));
        loan.setTermMonths(12);
        loan.setStatus(LoanStatus.PENDING);

        response = new LoanApplicationResponse();
        response.setId(loan.getId());
        response.setCustomerId(loan.getCustomerId());
        response.setPrincipal(loan.getPrincipal());
        response.setAnnualInterestRate(loan.getAnnualInterestRate());
        response.setTermMonths(loan.getTermMonths());
        response.setStatus(LoanStatus.PENDING);
    }

    @Test
    void create_returnsCreatedLoan() throws Exception {
        when(loanService.create(any(LoanApplication.class))).thenReturn(loan);
        when(mapper.toResponse(loan)).thenReturn(response);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "00000000-0000-0000-0000-000000000001",
                                  "principal": 1000.00,
                                  "annualInterestRate": 12.0,
                                  "termMonths": 12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.termMonths", is(12)));
    }

    @Test
    void list_returnsPagedLoans() throws Exception {
        when(loanService.list(PageRequest.of(0, 20), null)).thenReturn(new PageImpl<>(List.of(loan)));
        when(mapper.toPageResponse(any(), any())).thenCallRealMethod();
        when(mapper.toResponse(loan)).thenReturn(response);

        mockMvc.perform(get("/api/loans?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].principal", is(1000.00)));
    }

    @Test
    void approve_updatesStatus() throws Exception {
        loan.setStatus(LoanStatus.ACTIVE);
        response.setStatus(LoanStatus.ACTIVE);
        when(loanService.approve(eq(loan.getId()), any())).thenReturn(loan);
        when(mapper.toResponse(loan)).thenReturn(response);

        mockMvc.perform(post("/api/loans/{id}/approve", loan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}

