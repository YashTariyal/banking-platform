package com.banking.customer.service;

import com.banking.customer.domain.ContactInfo;
import com.banking.customer.domain.ContactType;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerPreferences;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import com.banking.customer.messaging.CustomerEventPublisher;
import com.banking.customer.repository.ContactInfoRepository;
import com.banking.customer.repository.CustomerPreferencesRepository;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.web.dto.CreateCustomerRequest;
import com.banking.customer.web.dto.UpdateCustomerRequest;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CustomerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerRepository customerRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final CustomerPreferencesRepository preferencesRepository;
    private final CustomerNumberGenerator customerNumberGenerator;
    private final CustomerEventPublisher eventPublisher;

    public CustomerService(
            CustomerRepository customerRepository,
            ContactInfoRepository contactInfoRepository,
            CustomerPreferencesRepository preferencesRepository,
            CustomerNumberGenerator customerNumberGenerator,
            CustomerEventPublisher eventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.contactInfoRepository = contactInfoRepository;
        this.preferencesRepository = preferencesRepository;
        this.customerNumberGenerator = customerNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    public Customer createCustomer(CreateCustomerRequest request) {
        // Check for duplicate national ID
        if (request.nationalId() != null && request.nationalIdType() != null) {
            customerRepository.findByNationalId(request.nationalId(), request.nationalIdType())
                    .ifPresent(c -> {
                        throw new IllegalArgumentException("Customer with national ID " + request.nationalId() + " already exists");
                    });
        }

        Customer customer = new Customer();
        customer.setCustomerNumber(customerNumberGenerator.generate());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setMiddleName(request.middleName());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());
        customer.setNationalId(request.nationalId());
        customer.setNationalIdType(request.nationalIdType());
        customer.setCustomerType(request.customerType() != null ? request.customerType() : CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.PENDING_VERIFICATION);
        customer.setKycStatus("PENDING");

        try {
            Customer saved = customerRepository.save(customer);

            // Create default preferences
            CustomerPreferences preferences = new CustomerPreferences();
            preferences.setCustomerId(saved.getId());
            preferences.setLanguage(request.language() != null ? request.language() : "en");
            preferences.setCurrency(request.currency() != null ? request.currency() : "USD");
            preferences.setTimezone(request.timezone() != null ? request.timezone() : "UTC");
            preferencesRepository.save(preferences);

            // Create contact info if provided
            if (request.email() != null || request.phone() != null || request.addressLine1() != null) {
                ContactInfo contactInfo = new ContactInfo();
                contactInfo.setCustomerId(saved.getId());
                contactInfo.setContactType(ContactType.HOME);
                contactInfo.setEmail(request.email());
                contactInfo.setPhone(request.phone());
                contactInfo.setMobile(request.mobile());
                contactInfo.setAddressLine1(request.addressLine1());
                contactInfo.setAddressLine2(request.addressLine2());
                contactInfo.setCity(request.city());
                contactInfo.setState(request.state());
                contactInfo.setPostalCode(request.postalCode());
                contactInfo.setCountry(request.country() != null ? request.country() : "US");
                contactInfo.setIsPrimary(true);
                contactInfoRepository.save(contactInfo);
            }

            eventPublisher.publishCustomerCreated(saved);
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Customer number or national ID already exists", e);
        }
    }

    public Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    public Customer getCustomerByNumber(String customerNumber) {
        return customerRepository.findByCustomerNumber(customerNumber)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerNumber));
    }

    public Page<Customer> listCustomers(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable validPageable = PageRequest.of(pageable.getPageNumber(), pageSize);
        return customerRepository.findAllActive(validPageable);
    }

    public Page<Customer> listCustomersByStatus(CustomerStatus status, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable validPageable = PageRequest.of(pageable.getPageNumber(), pageSize);
        return customerRepository.findByStatus(status, validPageable);
    }

    public Customer updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = getCustomer(id);

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }
        if (request.middleName() != null) {
            customer.setMiddleName(request.middleName());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            customer.setGender(request.gender());
        }
        if (request.status() != null) {
            customer.setStatus(request.status());
        }

        try {
            Customer updated = customerRepository.save(customer);
            eventPublisher.publishCustomerUpdated(updated);
            return updated;
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new IllegalStateException("Customer was modified by another transaction. Please retry.", e);
        }
    }

    public void deleteCustomer(UUID id) {
        Customer customer = getCustomer(id);
        customer.setDeletedAt(Instant.now());
        customer.setStatus(CustomerStatus.CLOSED);
        customerRepository.save(customer);
        eventPublisher.publishCustomerDeleted(customer);
    }

    public void updateKycStatus(UUID id, String kycStatus) {
        Customer customer = getCustomer(id);
        customer.setKycStatus(kycStatus);
        if ("VERIFIED".equals(kycStatus)) {
            customer.setKycVerifiedAt(Instant.now());
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        customerRepository.save(customer);
        eventPublisher.publishCustomerUpdated(customer);
    }

    public List<ContactInfo> getContactInfo(UUID customerId) {
        getCustomer(customerId); // Validate customer exists
        return contactInfoRepository.findByCustomerId(customerId);
    }

    public ContactInfo addContactInfo(UUID customerId, ContactInfo contactInfo) {
        getCustomer(customerId); // Validate customer exists
        contactInfo.setCustomerId(customerId);
        if (contactInfo.getIsPrimary() != null && contactInfo.getIsPrimary()) {
            // Unset other primary contacts
            contactInfoRepository.findByCustomerIdAndIsPrimaryTrue(customerId)
                    .ifPresent(primary -> {
                        primary.setIsPrimary(false);
                        contactInfoRepository.save(primary);
                    });
        }
        return contactInfoRepository.save(contactInfo);
    }

    public CustomerPreferences getPreferences(UUID customerId) {
        getCustomer(customerId); // Validate customer exists
        return preferencesRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerPreferences prefs = new CustomerPreferences();
                    prefs.setCustomerId(customerId);
                    return preferencesRepository.save(prefs);
                });
    }

    public CustomerPreferences updatePreferences(UUID customerId, CustomerPreferences preferences) {
        getCustomer(customerId); // Validate customer exists
        CustomerPreferences existing = preferencesRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerPreferences prefs = new CustomerPreferences();
                    prefs.setCustomerId(customerId);
                    return prefs;
                });

        if (preferences.getLanguage() != null) {
            existing.setLanguage(preferences.getLanguage());
        }
        if (preferences.getTimezone() != null) {
            existing.setTimezone(preferences.getTimezone());
        }
        if (preferences.getCurrency() != null) {
            existing.setCurrency(preferences.getCurrency());
        }
        if (preferences.getEmailNotificationsEnabled() != null) {
            existing.setEmailNotificationsEnabled(preferences.getEmailNotificationsEnabled());
        }
        if (preferences.getSmsNotificationsEnabled() != null) {
            existing.setSmsNotificationsEnabled(preferences.getSmsNotificationsEnabled());
        }
        if (preferences.getPushNotificationsEnabled() != null) {
            existing.setPushNotificationsEnabled(preferences.getPushNotificationsEnabled());
        }
        if (preferences.getMarketingEmailsEnabled() != null) {
            existing.setMarketingEmailsEnabled(preferences.getMarketingEmailsEnabled());
        }
        if (preferences.getPaperStatementsEnabled() != null) {
            existing.setPaperStatementsEnabled(preferences.getPaperStatementsEnabled());
        }
        if (preferences.getTwoFactorEnabled() != null) {
            existing.setTwoFactorEnabled(preferences.getTwoFactorEnabled());
        }
        if (preferences.getBiometricEnabled() != null) {
            existing.setBiometricEnabled(preferences.getBiometricEnabled());
        }
        if (preferences.getPreferredContactMethod() != null) {
            existing.setPreferredContactMethod(preferences.getPreferredContactMethod());
        }

        return preferencesRepository.save(existing);
    }
}

