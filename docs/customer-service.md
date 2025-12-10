## Customer Service

### 1. Overview
The customer service manages **customer PII (Personally Identifiable Information)**, **contact information**, and **preferences**.  
It exposes REST APIs for customer lifecycle management, persists data in PostgreSQL via Flyway migrations, and publishes Kafka events when customers are created, updated, or deleted.  
The service provides a foundation for other services to validate customer existence and retrieve customer information.

### 2. Running Locally
- **Prerequisites**
  - Java 21
  - Maven
  - Local Postgres (database `customer_service`, user `customer_svc`)
  - Local Kafka on `localhost:9092`

- **Start infra (if using the provided docker compose):**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/infrastructure
   docker compose up -d
   ```

- **Run customer-service:**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/services/customer-service
   mvn spring-boot:run
   ```

- **Endpoints & docs (default port 8081):**
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/api-docs`
- Health: `http://localhost:8081/actuator/health`

---

### 3. Features

#### 3.1 Customer Management
- **Customer Creation**: Create customers with PII, contact info, and default preferences
- **Customer Number Generation**: Auto-generates unique customer numbers (CUST + 12 digits)
- **Customer Updates**: Update customer information with optimistic locking
- **Soft Delete**: Soft delete customers (marks as deleted, sets status to CLOSED)
- **KYC Status Management**: Track and update KYC verification status

#### 3.2 Contact Information Management
- **Multiple Contact Methods**: Support for email, phone, mobile, and physical addresses
- **Contact Types**: HOME, WORK, BILLING, MAILING, OTHER
- **Primary Contact**: Designate primary contact information
- **Contact Verification**: Track verification status for contact methods

#### 3.3 Customer Preferences
- **Language & Locale**: Set preferred language, timezone, and currency
- **Notification Preferences**: Control email, SMS, push, and marketing notifications
- **Security Preferences**: Two-factor authentication and biometric settings
- **Statement Preferences**: Paper vs electronic statements

---

### 4. API Reference

All endpoints are documented in Swagger UI at `http://localhost:8081/swagger-ui.html`.

#### 4.1 Customers

- **POST** `/api/customers`  
  Create a new customer. Request:
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "middleName": "Middle",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "nationalId": "123-45-6789",
    "nationalIdType": "SSN",
    "customerType": "INDIVIDUAL",
    "email": "john.doe@example.com",
    "phone": "555-1234",
    "mobile": "555-5678",
    "addressLine1": "123 Main St",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US",
    "language": "en",
    "currency": "USD",
    "timezone": "America/New_York"
  }
  ```

- **GET** `/api/customers/{id}`  
  Get customer by ID.

- **GET** `/api/customers/number/{customerNumber}`  
  Get customer by customer number.

- **GET** `/api/customers?status=ACTIVE&page=0&size=20`  
  List customers with optional status filter.

- **PUT** `/api/customers/{id}`  
  Update customer. Request:
  ```json
  {
    "firstName": "Jane",
    "lastName": "Smith",
    "status": "ACTIVE"
  }
  ```

- **PUT** `/api/customers/{id}/kyc-status`  
  Update KYC status. Request:
  ```json
  {
    "kycStatus": "VERIFIED"
  }
  ```

- **DELETE** `/api/customers/{id}`  
  Soft delete customer (marks as deleted, sets status to CLOSED).

#### 4.2 Contact Information

- **GET** `/api/customers/{customerId}/contact-info`  
  Get all contact information for a customer.

- **POST** `/api/customers/{customerId}/contact-info`  
  Add contact information. Request:
  ```json
  {
    "contactType": "HOME",
    "email": "john@example.com",
    "phone": "555-1234",
    "mobile": "555-5678",
    "addressLine1": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US",
    "isPrimary": true
  }
  ```

#### 4.3 Customer Preferences

- **GET** `/api/customers/{customerId}/preferences`  
  Get customer preferences. Creates default preferences if none exist.

- **PUT** `/api/customers/{customerId}/preferences`  
  Update customer preferences. Request:
  ```json
  {
    "language": "es",
    "timezone": "America/Los_Angeles",
    "currency": "EUR",
    "emailNotificationsEnabled": true,
    "smsNotificationsEnabled": false,
    "pushNotificationsEnabled": true,
    "marketingEmailsEnabled": false,
    "paperStatementsEnabled": false,
    "twoFactorEnabled": true,
    "biometricEnabled": true,
    "preferredContactMethod": "EMAIL"
  }
  ```

---

### 5. Data Model (Flyway)

#### 5.1 customers
Stores customer PII and basic information:
- `id` (UUID, PK)
- `customer_number` (VARCHAR(32), unique, indexed)
- `status` (ENUM: ACTIVE, INACTIVE, SUSPENDED, CLOSED, PENDING_VERIFICATION, indexed)
- `first_name`, `last_name`, `middle_name` (VARCHAR(100))
- `date_of_birth` (DATE)
- `gender` (ENUM: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY)
- `national_id`, `national_id_type` (VARCHAR, indexed)
- `customer_type` (ENUM: INDIVIDUAL, BUSINESS, JOINT, indexed)
- `kyc_status` (VARCHAR(32))
- `kyc_verified_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)
- `version` (BIGINT, optimistic locking)
- `deleted_at` (TIMESTAMPTZ, soft delete)

#### 5.2 contact_info
Stores customer contact information:
- `id` (UUID, PK)
- `customer_id` (UUID, FK, indexed)
- `contact_type` (ENUM: HOME, WORK, BILLING, MAILING, OTHER)
- `email` (VARCHAR(255), indexed)
- `phone`, `mobile` (VARCHAR(50), indexed)
- `address_line1`, `address_line2` (VARCHAR(255))
- `city`, `state` (VARCHAR(100))
- `postal_code` (VARCHAR(20))
- `country` (CHAR(2), ISO 3166-1 alpha-2)
- `is_primary` (BOOLEAN, indexed with customer_id)
- `is_verified` (BOOLEAN)
- `verified_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.3 customer_preferences
Stores customer preferences:
- `id` (UUID, PK)
- `customer_id` (UUID, FK, unique, indexed)
- `language` (VARCHAR(10), ISO 639-1 code)
- `timezone` (VARCHAR(50))
- `currency` (CHAR(3), ISO 4217 code)
- `email_notifications_enabled` (BOOLEAN, default: true)
- `sms_notifications_enabled` (BOOLEAN, default: false)
- `push_notifications_enabled` (BOOLEAN, default: true)
- `marketing_emails_enabled` (BOOLEAN, default: false)
- `paper_statements_enabled` (BOOLEAN, default: false)
- `two_factor_enabled` (BOOLEAN, default: false)
- `biometric_enabled` (BOOLEAN)
- `preferred_contact_method` (VARCHAR(32))
- `created_at`, `updated_at` (TIMESTAMPTZ)

---

### 6. Kafka Integration

The service publishes events to Kafka when customers are created, updated, or deleted.

#### 6.1 Topics Published
- `customer-events`: Customer lifecycle events

#### 6.2 Event Types
- `CUSTOMER_CREATED`: Published when a new customer is created
- `CUSTOMER_UPDATED`: Published when customer information is updated
- `CUSTOMER_DELETED`: Published when a customer is deleted

#### 6.3 Event Format
```json
{
  "customerId": "<uuid>",
  "customerNumber": "CUST123456789012",
  "firstName": "John",
  "lastName": "Doe",
  "customerType": "INDIVIDUAL",
  "status": "ACTIVE",
  "kycStatus": "VERIFIED",
  "eventType": "CUSTOMER_CREATED",
  "occurredAt": "2024-01-15T10:30:00Z"
}
```

---

### 7. Common Responses

#### 7.1 PageResponse
Paginated responses follow this structure:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

#### 7.2 CustomerResponse
```json
{
  "id": "<uuid>",
  "customerNumber": "CUST123456789012",
  "status": "ACTIVE",
  "firstName": "John",
  "lastName": "Doe",
  "middleName": "Middle",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "nationalId": "123-45-6789",
  "nationalIdType": "SSN",
  "customerType": "INDIVIDUAL",
  "kycStatus": "VERIFIED",
  "kycVerifiedAt": "2024-01-15T10:30:00Z",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 7.3 ContactInfoResponse
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "contactType": "HOME",
  "email": "john@example.com",
  "phone": "555-1234",
  "mobile": "555-5678",
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postalCode": "10001",
  "country": "US",
  "isPrimary": true,
  "isVerified": true,
  "verifiedAt": "2024-01-15T10:30:00Z",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 7.4 CustomerPreferencesResponse
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "language": "en",
  "timezone": "America/New_York",
  "currency": "USD",
  "emailNotificationsEnabled": true,
  "smsNotificationsEnabled": false,
  "pushNotificationsEnabled": true,
  "marketingEmailsEnabled": false,
  "paperStatementsEnabled": false,
  "twoFactorEnabled": false,
  "biometricEnabled": true,
  "preferredContactMethod": "EMAIL",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

### 8. Error Handling

- **400 Bad Request**: Invalid request data (missing required fields, invalid enum values, duplicate national ID)
- **404 Not Found**: Customer not found
- **409 Conflict**: Concurrent update conflict (optimistic locking)
- **500 Internal Server Error**: Unexpected server errors

---

### 9. Testing

The service includes comprehensive test coverage:
- **Service layer tests**: Customer CRUD operations, contact info management, preferences management
- **Controller tests**: REST API endpoints

Run tests:
```bash
cd services/customer-service
mvn test
```

---

### 10. Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: customer-service
  datasource:
    url: jdbc:postgresql://localhost:5432/customer_service
    username: customer_svc
    password: changeMe!
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

### 11. Integration with Other Services

The customer service is used by other services for:
- **Account Service**: Validates customer existence before creating accounts
- **Card Service**: Retrieves customer information for card issuance
- **Compliance Service**: Uses customer data for AML monitoring
- **KYC Service**: Updates KYC status after verification

---

### 12. Next Steps

1. **Customer Search**: Add advanced search capabilities (by name, email, phone, etc.)
2. **Contact Verification**: Implement email/phone verification workflows
3. **Customer Segmentation**: Add customer segments and tags
4. **Audit Trail**: Enhanced audit logging for PII access
5. **Data Masking**: Mask sensitive PII in logs and responses
6. **GDPR Compliance**: Add data export and deletion capabilities

