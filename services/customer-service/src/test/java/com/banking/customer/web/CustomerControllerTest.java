package com.banking.customer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import com.banking.customer.domain.Gender;
import com.banking.customer.service.CustomerService;
import com.banking.customer.web.dto.CustomerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.banking.customer.config.PiiMaskingFilter;
import com.banking.customer.config.RequestLoggingFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import({RequestLoggingFilter.class, PiiMaskingFilter.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private CustomerMapper customerMapper;

    @MockBean
    private MeterRegistry meterRegistry;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createCustomer_CreatesNewCustomer() throws Exception {
        // Given
        com.banking.customer.web.dto.CreateCustomerRequest request =
                new com.banking.customer.web.dto.CreateCustomerRequest(
                        "John", "Doe", null, LocalDate.of(1990, 1, 1),
                        Gender.MALE, null, null, CustomerType.INDIVIDUAL,
                        "john@example.com", null, null, null, null, null, null, null, null,
                        "en", "USD", "UTC"
                );

        Customer customer = createCustomer();
        CustomerResponse response = createCustomerResponse(customer);

        when(customerService.createCustomer(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(customerService).createCustomer(any());
    }

    @Test
    void getCustomer_WhenExists_ReturnsCustomer() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);
        CustomerResponse response = createCustomerResponse(customer);

        when(customerService.getCustomer(id)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value(customer.getFirstName()));
    }

    @Test
    void listCustomers_ReturnsPaginatedResults() throws Exception {
        // Given
        Customer customer = createCustomer();
        Page<Customer> page = new PageImpl<>(List.of(customer));
        CustomerResponse response = createCustomerResponse(customer);

        when(customerService.listCustomers(any())).thenReturn(page);
        when(customerMapper.toResponse(customer)).thenReturn(response);
        when(customerMapper.toPageResponse(any(org.springframework.data.domain.Page.class))).thenAnswer(invocation -> {
            org.springframework.data.domain.Page<?> p = invocation.getArgument(0);
            return new com.banking.customer.web.dto.PageResponse<>(
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
        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void updateCustomer_UpdatesCustomer() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);
        customer.setFirstName("Jane");

        com.banking.customer.web.dto.UpdateCustomerRequest request =
                new com.banking.customer.web.dto.UpdateCustomerRequest(
                        "Jane", "Smith", null, null, Gender.FEMALE, CustomerStatus.ACTIVE
                );

        CustomerResponse response = createCustomerResponse(customer);

        when(customerService.updateCustomer(eq(id), any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));

        verify(customerService).updateCustomer(eq(id), any());
    }

    @Test
    void deleteCustomer_DeletesCustomer() throws Exception {
        // Given
        UUID id = UUID.randomUUID();

        // When/Then
        mockMvc.perform(delete("/api/customers/{id}", id))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(id);
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST123456789012");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCreatedAt(java.time.Instant.now());
        customer.setUpdatedAt(java.time.Instant.now());
        return customer;
    }

    private CustomerResponse createCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getStatus(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getMiddleName(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNationalId(),
                customer.getNationalIdType(),
                customer.getCustomerType(),
                customer.getKycStatus(),
                customer.getKycVerifiedAt(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

