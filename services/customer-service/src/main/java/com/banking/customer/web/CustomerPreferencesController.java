package com.banking.customer.web;

import com.banking.customer.domain.CustomerPreferences;
import com.banking.customer.service.CustomerService;
import com.banking.customer.web.dto.CustomerPreferencesResponse;
import com.banking.customer.web.dto.UpdatePreferencesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/{customerId}/preferences")
@Tag(name = "Customer Preferences", description = "Customer preferences management")
public class CustomerPreferencesController {

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    public CustomerPreferencesController(CustomerService customerService, CustomerMapper mapper) {
        this.customerService = customerService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            summary = "Get customer preferences",
            description = "Retrieves customer preferences. Creates default preferences if none exist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerPreferencesResponse> getPreferences(
            @Parameter(description = "Customer unique identifier", required = true)
            @PathVariable UUID customerId
    ) {
        CustomerPreferences preferences = customerService.getPreferences(customerId);
        return ResponseEntity.ok(mapper.toResponse(preferences));
    }

    @PutMapping
    @Operation(
            summary = "Update customer preferences",
            description = "Updates customer preferences. Only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerPreferencesResponse> updatePreferences(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        CustomerPreferences preferences = new CustomerPreferences();
        preferences.setLanguage(request.language());
        preferences.setTimezone(request.timezone());
        preferences.setCurrency(request.currency());
        preferences.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        preferences.setSmsNotificationsEnabled(request.smsNotificationsEnabled());
        preferences.setPushNotificationsEnabled(request.pushNotificationsEnabled());
        preferences.setMarketingEmailsEnabled(request.marketingEmailsEnabled());
        preferences.setPaperStatementsEnabled(request.paperStatementsEnabled());
        preferences.setTwoFactorEnabled(request.twoFactorEnabled());
        preferences.setBiometricEnabled(request.biometricEnabled());
        preferences.setPreferredContactMethod(request.preferredContactMethod());

        CustomerPreferences updated = customerService.updatePreferences(customerId, preferences);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}

