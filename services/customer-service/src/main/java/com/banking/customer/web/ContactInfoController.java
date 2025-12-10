package com.banking.customer.web;

import com.banking.customer.domain.ContactInfo;
import com.banking.customer.service.CustomerService;
import com.banking.customer.web.dto.ContactInfoResponse;
import com.banking.customer.web.dto.CreateContactInfoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/{customerId}/contact-info")
@Tag(name = "Contact Information", description = "Customer contact information management")
public class ContactInfoController {

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    public ContactInfoController(CustomerService customerService, CustomerMapper mapper) {
        this.customerService = customerService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            summary = "Get customer contact information",
            description = "Retrieves all contact information for a customer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<List<ContactInfoResponse>> getContactInfo(
            @Parameter(description = "Customer unique identifier", required = true)
            @PathVariable UUID customerId
    ) {
        List<ContactInfo> contactInfo = customerService.getContactInfo(customerId);
        return ResponseEntity.ok(contactInfo.stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add contact information",
            description = "Adds new contact information for a customer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact information added successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<ContactInfoResponse> addContactInfo(
            @PathVariable UUID customerId,
            @Valid @RequestBody CreateContactInfoRequest request
    ) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setContactType(request.contactType());
        contactInfo.setEmail(request.email());
        contactInfo.setPhone(request.phone());
        contactInfo.setMobile(request.mobile());
        contactInfo.setAddressLine1(request.addressLine1());
        contactInfo.setAddressLine2(request.addressLine2());
        contactInfo.setCity(request.city());
        contactInfo.setState(request.state());
        contactInfo.setPostalCode(request.postalCode());
        contactInfo.setCountry(request.country());
        contactInfo.setIsPrimary(request.isPrimary() != null ? request.isPrimary() : false);

        ContactInfo saved = customerService.addContactInfo(customerId, contactInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }
}

