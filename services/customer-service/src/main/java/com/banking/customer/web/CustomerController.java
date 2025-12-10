package com.banking.customer.web;

import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.service.CustomerService;
import com.banking.customer.web.dto.CreateCustomerRequest;
import com.banking.customer.web.dto.CustomerResponse;
import com.banking.customer.web.dto.PageResponse;
import com.banking.customer.web.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer management operations")
public class CustomerController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    public CustomerController(CustomerService customerService, CustomerMapper mapper) {
        this.customerService = customerService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new customer",
            description = "Creates a new customer with PII, contact information, and default preferences. Customer number is auto-generated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Customer with national ID already exists")
    })
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(customer));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves detailed information about a specific customer by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer unique identifier", required = true)
            @PathVariable UUID id
    ) {
        Customer customer = customerService.getCustomer(id);
        return ResponseEntity.ok(mapper.toResponse(customer));
    }

    @GetMapping("/number/{customerNumber}")
    @Operation(
            summary = "Get customer by customer number",
            description = "Retrieves customer information by customer number"
    )
    public ResponseEntity<CustomerResponse> getCustomerByNumber(
            @PathVariable String customerNumber
    ) {
        Customer customer = customerService.getCustomerByNumber(customerNumber);
        return ResponseEntity.ok(mapper.toResponse(customer));
    }

    @GetMapping
    @Operation(
            summary = "List customers",
            description = "Retrieves a paginated list of customers. Optionally filter by status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    public ResponseEntity<PageResponse<CustomerResponse>> listCustomers(
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) CustomerStatus status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Customer> customers = status != null
                ? customerService.listCustomersByStatus(status, pageable)
                : customerService.listCustomers(pageable);
        return ResponseEntity.ok(mapper.toPageResponse(customers.map(mapper::toResponse)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update customer",
            description = "Updates customer information. Only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict")
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(mapper.toResponse(customer));
    }

    @PutMapping("/{id}/kyc-status")
    @Operation(
            summary = "Update KYC status",
            description = "Updates the KYC verification status of a customer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerResponse> updateKycStatus(
            @PathVariable UUID id,
            @RequestBody UpdateKycStatusRequest request
    ) {
        customerService.updateKycStatus(id, request.kycStatus());
        Customer customer = customerService.getCustomer(id);
        return ResponseEntity.ok(mapper.toResponse(customer));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete customer",
            description = "Soft deletes a customer (marks as deleted, sets status to CLOSED)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public void deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }

    public record UpdateKycStatusRequest(String kycStatus) {
    }
}

