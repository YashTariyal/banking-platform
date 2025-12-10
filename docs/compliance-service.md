## Compliance Service

### 1. Overview
The compliance service provides **Anti-Money Laundering (AML) monitoring**, **suspicious activity detection**, and **regulatory reporting** capabilities.  
It consumes transaction events from Kafka, analyzes them for compliance risks, flags suspicious activities, and generates regulatory reports (SAR, CTR, LCTR).  
All compliance records are persisted in PostgreSQL via Flyway migrations, and the service exposes REST APIs for compliance officers to review and manage flagged activities.

### 2. Running Locally
- **Prerequisites**
  - Java 21
  - Maven
  - Local Postgres (database `compliance_service`, user `compliance_svc`)
  - Local Kafka on `localhost:9092`

- **Start infra (if using the provided docker compose):**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/infrastructure
   docker compose up -d
   ```

- **Run compliance-service:**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/services/compliance-service
   mvn spring-boot:run
   ```

- **Endpoints & docs (default port 8083):**
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/api-docs`
- Health: `http://localhost:8083/actuator/health`

---

### 3. Features

#### 3.1 AML Transaction Monitoring
The service automatically analyzes transactions from Kafka topics (`transaction-events`, `payment-events`, `card-events`) and performs risk scoring:

- **Large Cash Transaction Detection**: Flags transactions ≥ $10,000
- **Structuring Detection**: Identifies amounts just below reporting thresholds ($9,000-$9,999)
- **Round Number Pattern Detection**: Flags transactions with round numbers (potential structuring)
- **Rapid Movement Detection**: Identifies customers with excessive transaction frequency
- **Risk Scoring**: Calculates risk scores (0-100) based on multiple factors

#### 3.2 Suspicious Activity Management
Automatically creates suspicious activity records for high-risk transactions:
- **Activity Types**: Large cash transactions, structuring, rapid movement, unusual patterns, etc.
- **Severity Levels**: LOW, MEDIUM, HIGH, CRITICAL
- **Status Tracking**: OPEN, UNDER_INVESTIGATION, RESOLVED, FALSE_POSITIVE, ESCALATED, REPORTED_TO_AUTHORITIES
- **Investigation Workflow**: Assign investigators, add notes, update status

#### 3.3 Regulatory Reporting
Generates and submits regulatory reports:
- **SAR (Suspicious Activity Report)**: Reports suspicious activities to authorities
- **CTR (Currency Transaction Report)**: Reports large cash transactions
- **LCTR (Large Cash Transaction Report)**: Specific large transaction reports
- **Report Status**: DRAFT, GENERATED, SUBMITTED, ACCEPTED, REJECTED

---

### 4. API Reference

All endpoints are documented in Swagger UI at `http://localhost:8083/swagger-ui.html`.

#### 4.1 Compliance Records

- **GET** `/api/compliance/records/{id}`  
  Get a specific compliance record by ID.

- **GET** `/api/compliance/records/customer/{customerId}?page=0&size=20`  
  Get paginated compliance records for a customer.

- **GET** `/api/compliance/records/account/{accountId}?page=0&size=20`  
  Get paginated compliance records for an account.

- **GET** `/api/compliance/records?recordType=TRANSACTION_MONITORING&status=FLAGGED&page=0&size=20`  
  List compliance records with optional filters (recordType, status).

- **PUT** `/api/compliance/records/{id}/status`  
  Update compliance record status. Request:
  ```json
  { "status": "PENDING|UNDER_REVIEW|CLEARED|FLAGGED|ESCALATED|REPORTED" }
  ```

#### 4.2 Suspicious Activities

- **GET** `/api/compliance/suspicious-activities/{id}`  
  Get a specific suspicious activity by ID.

- **GET** `/api/compliance/suspicious-activities?customerId={uuid}&status=OPEN&severity=HIGH&page=0&size=20`  
  List suspicious activities with optional filters (customerId, status, severity, activityType).

- **PUT** `/api/compliance/suspicious-activities/{id}`  
  Update suspicious activity (status, investigator, notes). Request:
  ```json
  {
    "status": "UNDER_INVESTIGATION",
    "investigatorId": "<uuid>",
    "investigationNotes": "Investigation in progress"
  }
  ```

#### 4.3 Regulatory Reports

- **POST** `/api/compliance/reports`  
  Generate a new regulatory report. Request:
  ```json
  {
    "reportType": "SAR|CTR|LCTR|AML_MONTHLY|AML_QUARTERLY|AML_ANNUAL",
    "reportPeriodStart": "2024-01-01",
    "reportPeriodEnd": "2024-01-31"
  }
  ```

- **GET** `/api/compliance/reports/{id}`  
  Get a specific regulatory report by ID.

- **GET** `/api/compliance/reports?reportType=SAR&status=DRAFT&page=0&size=20`  
  List regulatory reports with optional filters (reportType, status).

- **PUT** `/api/compliance/reports/{id}/submit`  
  Submit a regulatory report to authorities. Request:
  ```json
  {
    "submittedBy": "<uuid>",
    "regulatoryReference": "REF-12345"
  }
  ```

---

### 5. Data Model (Flyway)

#### 5.1 compliance_records
Tracks all compliance-related events and transactions:
- `id` (UUID, PK)
- `customer_id` (UUID, indexed)
- `account_id` (UUID, indexed)
- `transaction_id` (UUID, indexed)
- `record_type` (ENUM: TRANSACTION_MONITORING, LARGE_CASH_TRANSACTION, SUSPICIOUS_ACTIVITY, etc.)
- `status` (ENUM: PENDING, UNDER_REVIEW, CLEARED, FLAGGED, ESCALATED, REPORTED)
- `amount` (NUMERIC)
- `currency` (CHAR(3))
- `description` (TEXT)
- `risk_score` (INTEGER)
- `flags` (TEXT, JSON array of flag types)
- `source_event_type` (VARCHAR)
- `source_topic` (VARCHAR)
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.2 suspicious_activities
Flags suspicious activities for investigation:
- `id` (UUID, PK)
- `customer_id` (UUID, indexed)
- `account_id` (UUID)
- `transaction_id` (UUID)
- `activity_type` (ENUM: LARGE_CASH_TRANSACTION, STRUCTURING, RAPID_MOVEMENT, etc.)
- `severity` (ENUM: LOW, MEDIUM, HIGH, CRITICAL)
- `status` (ENUM: OPEN, UNDER_INVESTIGATION, RESOLVED, etc.)
- `amount` (NUMERIC)
- `currency` (CHAR(3))
- `description` (TEXT)
- `risk_score` (INTEGER)
- `compliance_record_id` (UUID, FK)
- `investigator_id` (UUID)
- `investigation_notes` (TEXT)
- `reported_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.3 regulatory_reports
Stores regulatory report metadata:
- `id` (UUID, PK)
- `report_type` (ENUM: CTR, SAR, LCTR, AML_MONTHLY, etc.)
- `report_period_start`, `report_period_end` (DATE, indexed)
- `status` (ENUM: DRAFT, GENERATED, SUBMITTED, ACCEPTED, REJECTED)
- `file_path` (VARCHAR)
- `record_count` (INTEGER)
- `total_amount` (NUMERIC)
- `submitted_at` (TIMESTAMPTZ)
- `submitted_by` (UUID)
- `regulatory_reference` (VARCHAR)
- `notes` (TEXT)
- `created_at`, `updated_at` (TIMESTAMPTZ)

---

### 6. Kafka Integration

The service consumes events from multiple Kafka topics:

#### 6.1 Topics Consumed
- `transaction-events`: Transaction completion events
- `payment-events`: Payment processing events
- `card-events`: Card transaction events

#### 6.2 Event Processing
- Automatically extracts `customerId`, `accountId`, `transactionId`, `amount`, `currency` from events
- Performs AML analysis on each transaction
- Creates compliance records and suspicious activities as needed
- Handles missing or invalid fields gracefully

#### 6.3 Event Format
Expected event structure (JSON):
```json
{
  "eventType": "TRANSACTION_COMPLETED",
  "customerId": "<uuid>",
  "accountId": "<uuid>",
  "transactionId": "<uuid>",
  "amount": "15000.00",
  "currency": "USD"
}
```

---

### 7. Risk Scoring Algorithm

The AML service calculates risk scores based on multiple factors:

| Factor | Points | Threshold |
|--------|--------|-----------|
| Large Cash Transaction (≥$10,000) | +30 | - |
| Structuring ($9,000-$9,999) | +40 | - |
| Round Number Pattern | +10 | - |
| Rapid Movement (>5 recent transactions) | +20 | - |

**Risk Score Thresholds:**
- **HIGH** (≥80): Status = FLAGGED, Severity = HIGH
- **MEDIUM** (≥50): Status = UNDER_REVIEW, Severity = MEDIUM
- **LOW** (≥20): Status = PENDING
- **CLEAR** (<20): Status = CLEARED

---

### 8. Common Responses

#### 8.1 PageResponse
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

#### 8.2 ComplianceRecordResponse
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "accountId": "<uuid>",
  "transactionId": "<uuid>",
  "recordType": "LARGE_CASH_TRANSACTION",
  "status": "FLAGGED",
  "amount": 15000.00,
  "currency": "USD",
  "description": "AML Analysis - Risk Score: 60, Flags: LARGE_CASH_TRANSACTION, ROUND_NUMBER_PATTERN",
  "riskScore": 60,
  "flags": "LARGE_CASH_TRANSACTION,ROUND_NUMBER_PATTERN",
  "sourceEventType": "TRANSACTION_COMPLETED",
  "sourceTopic": "transaction-events",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 8.3 SuspiciousActivityResponse
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "accountId": "<uuid>",
  "transactionId": "<uuid>",
  "activityType": "LARGE_CASH_TRANSACTION",
  "severity": "HIGH",
  "status": "OPEN",
  "amount": 15000.00,
  "currency": "USD",
  "description": "AML Analysis - Risk Score: 85",
  "riskScore": 85,
  "complianceRecordId": "<uuid>",
  "investigatorId": null,
  "investigationNotes": null,
  "reportedAt": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 8.4 RegulatoryReportResponse
```json
{
  "id": "<uuid>",
  "reportType": "SAR",
  "reportPeriodStart": "2024-01-01",
  "reportPeriodEnd": "2024-01-31",
  "status": "SUBMITTED",
  "filePath": "/reports/sar-2024-01.pdf",
  "recordCount": 25,
  "totalAmount": 500000.00,
  "submittedAt": "2024-02-01T09:00:00Z",
  "submittedBy": "<uuid>",
  "regulatoryReference": "REF-12345",
  "notes": "Monthly SAR submission",
  "createdAt": "2024-01-31T23:00:00Z",
  "updatedAt": "2024-02-01T09:00:00Z"
}
```

---

### 9. Error Handling

- **400 Bad Request**: Invalid request data (missing required fields, invalid enum values)
- **404 Not Found**: Resource not found (compliance record, suspicious activity, or report)
- **409 Conflict**: Duplicate report generation for the same period
- **500 Internal Server Error**: Unexpected server errors

---

### 10. Testing

The service includes comprehensive test coverage:
- **51 tests** covering all major functionality
- **Service layer tests**: AML analysis, compliance management, report generation
- **Controller tests**: REST API endpoints
- **Kafka consumer tests**: Event processing and error handling

Run tests:
```bash
cd services/compliance-service
mvn test
```

---

### 11. Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: compliance-service
  datasource:
    url: jdbc:postgresql://localhost:5432/compliance_service
    username: compliance_svc
    password: changeMe!
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: compliance-service
      auto-offset-reset: earliest

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

### 12. Next Steps

1. **Enhanced Risk Scoring**: Add machine learning models for more sophisticated risk assessment
2. **Sanctions Screening**: Integrate with sanctions/PEP screening services
3. **Report Export**: Generate PDF/CSV exports for regulatory submissions
4. **Real-time Alerts**: Push notifications for critical suspicious activities
5. **Case Management**: Advanced workflow for investigation management
6. **Audit Trail**: Enhanced audit logging for compliance officer actions

