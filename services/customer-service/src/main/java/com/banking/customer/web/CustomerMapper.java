package com.banking.customer.web;

import com.banking.customer.domain.ContactInfo;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerPreferences;
import com.banking.customer.web.dto.ContactInfoResponse;
import com.banking.customer.web.dto.CustomerPreferencesResponse;
import com.banking.customer.web.dto.CustomerResponse;
import com.banking.customer.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
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

    public ContactInfoResponse toResponse(ContactInfo contactInfo) {
        return new ContactInfoResponse(
                contactInfo.getId(),
                contactInfo.getCustomerId(),
                contactInfo.getContactType(),
                contactInfo.getEmail(),
                contactInfo.getPhone(),
                contactInfo.getMobile(),
                contactInfo.getAddressLine1(),
                contactInfo.getAddressLine2(),
                contactInfo.getCity(),
                contactInfo.getState(),
                contactInfo.getPostalCode(),
                contactInfo.getCountry(),
                contactInfo.getIsPrimary(),
                contactInfo.getIsVerified(),
                contactInfo.getVerifiedAt(),
                contactInfo.getCreatedAt(),
                contactInfo.getUpdatedAt()
        );
    }

    public CustomerPreferencesResponse toResponse(CustomerPreferences preferences) {
        return new CustomerPreferencesResponse(
                preferences.getId(),
                preferences.getCustomerId(),
                preferences.getLanguage(),
                preferences.getTimezone(),
                preferences.getCurrency(),
                preferences.getEmailNotificationsEnabled(),
                preferences.getSmsNotificationsEnabled(),
                preferences.getPushNotificationsEnabled(),
                preferences.getMarketingEmailsEnabled(),
                preferences.getPaperStatementsEnabled(),
                preferences.getTwoFactorEnabled(),
                preferences.getBiometricEnabled(),
                preferences.getPreferredContactMethod(),
                preferences.getCreatedAt(),
                preferences.getUpdatedAt()
        );
    }

    public <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

