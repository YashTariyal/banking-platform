package com.banking.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.customer.domain.ContactInfo;
import com.banking.customer.domain.ContactType;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerPreferences;
import com.banking.customer.domain.CustomerStatus;
import com.banking.customer.domain.CustomerType;
import com.banking.customer.domain.Gender;
import com.banking.customer.messaging.CustomerEventPublisher;
import com.banking.customer.repository.ContactInfoRepository;
import com.banking.customer.repository.CustomerPreferencesRepository;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.web.dto.CreateCustomerRequest;
import com.banking.customer.web.dto.UpdateCustomerRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContactInfoRepository contactInfoRepository;

    @Mock
    private CustomerPreferencesRepository preferencesRepository;

    @Mock
    private CustomerNumberGenerator customerNumberGenerator;

    @Mock
    private CustomerEventPublisher eventPublisher;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository,
                contactInfoRepository,
                preferencesRepository,
                customerNumberGenerator,
                eventPublisher
        );
    }

    @Test
    void createCustomer_GeneratesCustomerNumberAndSaves() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John",
                "Doe",
                "Middle",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "123-45-6789",
                "SSN",
                CustomerType.INDIVIDUAL,
                "john.doe@example.com",
                "555-1234",
                "555-5678",
                "123 Main St",
                "Apt 4B",
                "New York",
                "NY",
                "10001",
                "US",
                "en",
                "USD",
                "America/New_York"
        );

        String customerNumber = "CUST123456789012";
        UUID customerId = UUID.randomUUID();
        when(customerNumberGenerator.generate()).thenReturn(customerNumber);
        when(customerRepository.findByNationalId("123-45-6789", "SSN")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(customerId);
            return c;
        });
        when(preferencesRepository.save(any(CustomerPreferences.class))).thenAnswer(invocation -> {
            CustomerPreferences prefs = invocation.getArgument(0);
            prefs.setId(UUID.randomUUID());
            return prefs;
        });
        when(contactInfoRepository.save(any(ContactInfo.class))).thenAnswer(invocation -> {
            ContactInfo ci = invocation.getArgument(0);
            ci.setId(UUID.randomUUID());
            return ci;
        });

        // When
        Customer customer = customerService.createCustomer(request);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getCustomerNumber()).isEqualTo(customerNumber);
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        assertThat(customer.getKycStatus()).isEqualTo("PENDING");
        verify(customerRepository).save(any(Customer.class));
        verify(preferencesRepository).save(any(CustomerPreferences.class));
        verify(contactInfoRepository).save(any(ContactInfo.class));
        verify(eventPublisher).publishCustomerCreated(any(Customer.class));
    }

    @Test
    void createCustomer_WithDuplicateNationalId_ThrowsException() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John", "Doe", null, LocalDate.of(1990, 1, 1),
                Gender.MALE, "123-45-6789", "SSN", CustomerType.INDIVIDUAL,
                null, null, null, null, null, null, null, null, null,
                null, null, null
        );

        Customer existing = new Customer();
        when(customerRepository.findByNationalId("123-45-6789", "SSN")).thenReturn(Optional.of(existing));

        // When/Then
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getCustomer_WhenExists_ReturnsCustomer() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        // When
        Customer result = customerService.getCustomer(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getCustomer_WhenNotExists_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getCustomer_WhenDeleted_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);
        customer.setDeletedAt(java.time.Instant.now());

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        // When/Then
        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateCustomer_UpdatesFields() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);

        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jane", "Smith", null, null, Gender.FEMALE, CustomerStatus.ACTIVE
        );

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Customer updated = customerService.updateCustomer(id, request);

        // Then
        assertThat(updated.getFirstName()).isEqualTo("Jane");
        assertThat(updated.getLastName()).isEqualTo("Smith");
        assertThat(updated.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(updated.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        verify(eventPublisher).publishCustomerUpdated(any(Customer.class));
    }

    @Test
    void deleteCustomer_SoftDeletes() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        customerService.deleteCustomer(id);

        // Then
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.CLOSED);
        verify(eventPublisher).publishCustomerDeleted(any(Customer.class));
    }

    @Test
    void updateKycStatus_WhenVerified_SetsActiveStatus() {
        // Given
        UUID id = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(id);

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        customerService.updateKycStatus(id, "VERIFIED");

        // Then
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getKycStatus()).isEqualTo("VERIFIED");
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(captor.getValue().getKycVerifiedAt()).isNotNull();
    }

    @Test
    void getContactInfo_ReturnsContactInfo() {
        // Given
        UUID customerId = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(customerId);
        ContactInfo contactInfo = createContactInfo();
        contactInfo.setCustomerId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contactInfoRepository.findByCustomerId(customerId)).thenReturn(List.of(contactInfo));

        // When
        List<ContactInfo> result = customerService.getContactInfo(customerId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void addContactInfo_WithPrimary_UnsetsOtherPrimary() {
        // Given
        UUID customerId = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(customerId);

        ContactInfo existingPrimary = createContactInfo();
        existingPrimary.setCustomerId(customerId);
        existingPrimary.setIsPrimary(true);

        ContactInfo newPrimary = createContactInfo();
        newPrimary.setIsPrimary(true);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contactInfoRepository.findByCustomerIdAndIsPrimaryTrue(customerId))
                .thenReturn(Optional.of(existingPrimary));
        when(contactInfoRepository.save(any(ContactInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        customerService.addContactInfo(customerId, newPrimary);

        // Then
        verify(contactInfoRepository, org.mockito.Mockito.atLeastOnce()).save(any(ContactInfo.class));
    }

    @Test
    void getPreferences_CreatesDefaultIfNotExists() {
        // Given
        UUID customerId = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(preferencesRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(CustomerPreferences.class))).thenAnswer(invocation -> {
            CustomerPreferences prefs = invocation.getArgument(0);
            if (prefs.getId() == null) {
                prefs.setId(UUID.randomUUID());
            }
            if (prefs.getCreatedAt() == null) {
                prefs.setCreatedAt(java.time.Instant.now());
            }
            if (prefs.getUpdatedAt() == null) {
                prefs.setUpdatedAt(java.time.Instant.now());
            }
            // Ensure defaults are set
            if (prefs.getEmailNotificationsEnabled() == null) {
                prefs.setEmailNotificationsEnabled(true);
            }
            return prefs;
        });

        // When
        CustomerPreferences preferences = customerService.getPreferences(customerId);

        // Then
        assertThat(preferences).isNotNull();
        assertThat(preferences.getCustomerId()).isEqualTo(customerId);
        assertThat(preferences.getEmailNotificationsEnabled()).isTrue();
        verify(preferencesRepository).save(any(CustomerPreferences.class));
    }

    @Test
    void updatePreferences_UpdatesExistingPreferences() {
        // Given
        UUID customerId = UUID.randomUUID();
        Customer customer = createCustomer();
        customer.setId(customerId);

        CustomerPreferences existing = new CustomerPreferences();
        existing.setCustomerId(customerId);
        existing.setLanguage("en");

        CustomerPreferences updates = new CustomerPreferences();
        updates.setLanguage("es");
        updates.setCurrency("EUR");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(preferencesRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(CustomerPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CustomerPreferences updated = customerService.updatePreferences(customerId, updates);

        // Then
        assertThat(updated.getLanguage()).isEqualTo("es");
        assertThat(updated.getCurrency()).isEqualTo("EUR");
        verify(preferencesRepository).save(existing);
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

    private ContactInfo createContactInfo() {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setId(UUID.randomUUID());
        contactInfo.setContactType(ContactType.HOME);
        contactInfo.setEmail("test@example.com");
        contactInfo.setIsPrimary(false);
        contactInfo.setCreatedAt(java.time.Instant.now());
        contactInfo.setUpdatedAt(java.time.Instant.now());
        return contactInfo;
    }
}

