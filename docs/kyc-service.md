## KYC Service

### 1. Overview
The KYC service manages **Know Your Customer (KYC) onboarding workflows**, **document verification**, and **screening** (sanctions, PEP, adverse media).  
It automatically creates KYC cases when customers are registered, tracks document uploads and verification, performs screening checks, and manages the KYC approval workflow.  
The service publishes Kafka events for KYC lifecycle events and integrates with customer and compliance services.

### 2. Running Locally
- **Prerequisites**
  - Java 21
  - Maven
  - Local Postgres (database `kyc_service`, user `kyc_svc`)
  - Local Kafka on `localhost:9092`

- **Start infra (if using the provided docker compose):**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/infrastructure
   docker compose up -d
   ```

- **Run kyc-service:**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/services/kyc-service
   mvn spring-boot:run
   ```

- **Endpoints & docs (default port 8084):**
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/api-docs`
- Health: `http://localhost:8084/actuator/health`

---

### 3. Features

#### 3.1 KYC Case Management
- **Automatic Case Creation**: Creates onboarding KYC cases when customers are registered (via Kafka)
- **Case Types**: ONBOARDING, PERIODIC_REVIEW, ENHANCED_DUE_DILIGENCE
- **Status Tracking**: PENDING, IN_PROGRESS, DOCUMENT_REVIEW, SCREENING, UNDER_REVIEW, APPROVED, REJECTED, ON_HOLD
- **Risk Level Assignment**: LOW, MEDIUM, HIGH, CRITICAL
- **Case Assignment**: Assign cases to reviewers/analysts
- **Due Date Management**: Track and manage case due dates

#### 3.2 Document Verification
- **Document Upload**: Upload various document types (passport, driver's license, proof of address, etc.)
- **Document Types**: PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, TAX_ID, PROOF_OF_ADDRESS, BANK_STATEMENT, UTILITY_BILL, OTHER
- **Verification Workflow**: PENDING → VERIFIED/REJECTED/EXPIRED
- **Automatic Status Update**: KYC case status automatically updates when all documents are verified
- **Document Metadata**: Track document numbers, expiry dates, issuing countries

#### 3.3 Screening
- **Screening Types**: SANCTIONS, PEP (Politically Exposed Person), ADVERSE_MEDIA, WATCHLIST, COMPREHENSIVE
- **Screening Results**: CLEAR, MATCH, FALSE_POSITIVE, REQUIRES_REVIEW
- **Match Scoring**: Match scores (0-100) for screening results
- **Review Workflow**: Review and mark matches as false positives or confirmed matches
- **Automatic Completion**: KYC case status automatically updates when all required screenings are completed

#### 3.4 Approval Workflow
- **Approval Requirements**: Both screening and document verification must be completed
- **Approval Process**: Approve or reject KYC cases with notes/reasons
- **Status Updates**: Automatically updates customer KYC status in customer service (via Kafka)

---

### 4. API Reference

All endpoints are documented in Swagger UI at `http://localhost:8084/swagger-ui.html`.

#### 4.1 KYC Cases

- **POST** `/api/kyc/cases`  
  Create a new KYC case. Request:
  ```json
  {
    "customerId": "<uuid>",
    "caseType": "ONBOARDING",
    "riskLevel": "LOW",
    "assignedTo": "<uuid>",
    "dueDate": "2024-12-31T23:59:59Z"
  }
  ```

- **GET** `/api/kyc/cases/{id}`  
  Get KYC case by ID.

- **GET** `/api/kyc/cases?customerId=<uuid>&status=PENDING&page=0&size=20`  
  List KYC cases with optional filters.

- **PUT** `/api/kyc/cases/{id}`  
  Update KYC case. Request:
  ```json
  {
    "status": "UNDER_REVIEW",
    "riskLevel": "MEDIUM",
    "reviewNotes": "Additional review required"
  }
  ```

- **POST** `/api/kyc/cases/{id}/approve`  
  Approve KYC case. Request:
  ```json
  {
    "approvedBy": "<uuid>",
    "notes": "All requirements met"
  }
  ```

- **POST** `/api/kyc/cases/{id}/reject`  
  Reject KYC case. Request:
  ```json
  {
    "rejectedBy": "<uuid>",
    "reason": "Insufficient documentation"
  }
  ```

#### 4.2 Documents

- **POST** `/api/kyc/documents`  
  Upload a document. Request:
  ```json
  {
    "kycCaseId": "<uuid>",
    "documentType": "PASSPORT",
    "fileName": "passport.pdf",
    "filePath": "/uploads/passport.pdf",
    "fileSize": 1024000,
    "mimeType": "application/pdf",
    "documentNumber": "P123456",
    "issuingCountry": "US",
    "expiryDate": "2025-12-31T23:59:59Z"
  }
  ```

- **GET** `/api/kyc/documents/case/{kycCaseId}?documentType=PASSPORT`  
  Get documents for a KYC case (optionally filter by type).

- **PUT** `/api/kyc/documents/{id}/verify`  
  Verify a document. Request:
  ```json
  {
    "verificationStatus": "VERIFIED",
    "verifiedBy": "<uuid>",
    "verificationNotes": "Document verified"
  }
  ```

#### 4.3 Screening

- **POST** `/api/kyc/screening`  
  Perform screening. Request:
  ```json
  {
    "kycCaseId": "<uuid>",
    "customerId": "<uuid>",
    "screeningType": "SANCTIONS",
    "result": "CLEAR",
    "matchScore": 0,
    "screeningProvider": "INTERNAL",
    "screeningReference": "REF123"
  }
  ```

- **GET** `/api/kyc/screening/case/{kycCaseId}`  
  Get screening results for a KYC case.

- **PUT** `/api/kyc/screening/{id}/review`  
  Review a screening result. Request:
  ```json
  {
    "reviewedBy": "<uuid>",
    "reviewNotes": "False positive",
    "finalResult": "FALSE_POSITIVE"
  }
  ```

---

### 5. Data Model (Flyway)

#### 5.1 kyc_cases
Stores KYC case information:
- `id` (UUID, PK)
- `customer_id` (UUID, FK, indexed)
- `status` (ENUM, indexed)
- `risk_level` (ENUM)
- `case_type` (VARCHAR(32), indexed)
- `assigned_to` (UUID, indexed)
- `review_notes` (TEXT)
- `screening_completed` (BOOLEAN, default: false)
- `document_verification_completed` (BOOLEAN, default: false)
- `approved_at`, `rejected_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `due_date` (TIMESTAMPTZ, indexed)
- `completed_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)
- `version` (BIGINT, optimistic locking)

#### 5.2 documents
Stores document information:
- `id` (UUID, PK)
- `kyc_case_id` (UUID, FK, indexed)
- `document_type` (ENUM, indexed)
- `file_name` (VARCHAR(255))
- `file_path` (VARCHAR(500))
- `file_size` (BIGINT)
- `mime_type` (VARCHAR(100))
- `verification_status` (ENUM, indexed)
- `verified_at`, `verified_by` (TIMESTAMPTZ, UUID)
- `verification_notes` (TEXT)
- `expiry_date` (TIMESTAMPTZ)
- `document_number` (VARCHAR(100))
- `issuing_country` (CHAR(2))
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.3 screening_results
Stores screening results:
- `id` (UUID, PK)
- `kyc_case_id` (UUID, FK, indexed)
- `customer_id` (UUID, FK, indexed)
- `screening_type` (ENUM, indexed)
- `result` (ENUM, indexed)
- `match_score` (INTEGER)
- `matched_name` (VARCHAR(255))
- `matched_list` (VARCHAR(100))
- `match_details` (TEXT)
- `screening_provider` (VARCHAR(100))
- `screening_reference` (VARCHAR(255))
- `reviewed_at`, `reviewed_by` (TIMESTAMPTZ, UUID)
- `review_notes` (TEXT)
- `created_at`, `updated_at` (TIMESTAMPTZ)

---

### 6. Kafka Integration

The service consumes and publishes events to Kafka.

#### 6.1 Topics Consumed
- `customer-events`: Listens for `CUSTOMER_CREATED` events to automatically create onboarding KYC cases

#### 6.2 Topics Published
- `kyc-events`: KYC lifecycle events

#### 6.3 Event Types Published
- `KYC_CASE_CREATED`: Published when a new KYC case is created
- `KYC_CASE_UPDATED`: Published when a KYC case is updated
- `KYC_CASE_APPROVED`: Published when a KYC case is approved
- `KYC_CASE_REJECTED`: Published when a KYC case is rejected

#### 6.4 Event Format
```json
{
  "kycCaseId": "<uuid>",
  "customerId": "<uuid>",
  "status": "APPROVED",
  "riskLevel": "LOW",
  "caseType": "ONBOARDING",
  "eventType": "KYC_CASE_APPROVED",
  "occurredAt": "2024-01-15T10:30:00Z"
}
```

---

### 7. Workflow

#### 7.1 Onboarding Workflow
1. Customer registers → `CUSTOMER_CREATED` event published
2. KYC service automatically creates onboarding KYC case (status: PENDING)
3. Customer uploads required documents (status: DOCUMENT_REVIEW)
4. Documents are verified (status: SCREENING when all verified)
5. Screening is performed (SANCTIONS, PEP) (status: UNDER_REVIEW when all completed)
6. Reviewer approves or rejects the case
7. If approved, customer KYC status is updated to VERIFIED (via Kafka)

#### 7.2 Document Verification Workflow
1. Document uploaded → status: PENDING
2. Reviewer verifies document → status: VERIFIED/REJECTED
3. When all documents are verified → KYC case status updates to SCREENING

#### 7.3 Screening Workflow
1. Screening performed → result: CLEAR/MATCH/REQUIRES_REVIEW
2. If MATCH → reviewer reviews and marks as FALSE_POSITIVE or confirmed
3. When all required screenings completed → KYC case status updates to UNDER_REVIEW

---

### 8. Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8084

spring:
  application:
    name: kyc-service
  datasource:
    url: jdbc:postgresql://localhost:5432/kyc_service
    username: kyc_svc
    password: changeMe!
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: kyc-service
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

### 9. Common Responses

#### 9.1 KYCCaseResponse
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "status": "UNDER_REVIEW",
  "riskLevel": "LOW",
  "caseType": "ONBOARDING",
  "assignedTo": "<uuid>",
  "reviewNotes": "Review in progress",
  "screeningCompleted": true,
  "documentVerificationCompleted": true,
  "approvedAt": null,
  "rejectedAt": null,
  "rejectionReason": null,
  "dueDate": "2024-12-31T23:59:59Z",
  "completedAt": null,
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 9.2 DocumentResponse
```json
{
  "id": "<uuid>",
  "kycCaseId": "<uuid>",
  "documentType": "PASSPORT",
  "fileName": "passport.pdf",
  "filePath": "/uploads/passport.pdf",
  "fileSize": 1024000,
  "mimeType": "application/pdf",
  "verificationStatus": "VERIFIED",
  "verifiedAt": "2024-01-15T10:30:00Z",
  "verifiedBy": "<uuid>",
  "verificationNotes": "Document verified",
  "expiryDate": "2025-12-31T23:59:59Z",
  "documentNumber": "P123456",
  "issuingCountry": "US",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 9.3 ScreeningResultResponse
```json
{
  "id": "<uuid>",
  "kycCaseId": "<uuid>",
  "customerId": "<uuid>",
  "screeningType": "SANCTIONS",
  "result": "CLEAR",
  "matchScore": 0,
  "matchedName": null,
  "matchedList": null,
  "matchDetails": null,
  "screeningProvider": "INTERNAL",
  "screeningReference": "REF123",
  "reviewedAt": null,
  "reviewedBy": null,
  "reviewNotes": null,
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z"
}
```

---

### 10. Error Handling

- **400 Bad Request**: Invalid request data, missing required fields
- **404 Not Found**: KYC case not found, document not found
- **409 Conflict**: Active KYC case already exists for customer
- **500 Internal Server Error**: Unexpected server errors

---

### 11. Testing

The service includes comprehensive test coverage:
- **Service layer tests**: KYC case management, document verification, screening
- **Controller tests**: REST API endpoints (to be added)

Run tests:
```bash
cd services/kyc-service
mvn test
```

---

### 12. Integration with Other Services

The KYC service integrates with:
- **Customer Service**: Listens for customer creation events, updates customer KYC status
- **Compliance Service**: Shares screening results for AML monitoring
- **Identity Service**: Links KYC cases to user accounts

---

### 13. Next Steps

1. **External Screening Integration**: Integrate with external screening providers (e.g., Dow Jones, World-Check)
2. **Document Storage**: Integrate with object storage (S3, Azure Blob) for document files
3. **OCR Integration**: Automatic document data extraction using OCR
4. **Workflow Engine**: Implement configurable workflow engine for different case types
5. **Periodic Reviews**: Automated periodic KYC reviews based on risk level
6. **Enhanced Due Diligence**: Support for EDD workflows for high-risk customers
7. **Audit Trail**: Enhanced audit logging for all KYC activities
8. **Notifications**: Email/SMS notifications for case status updates

