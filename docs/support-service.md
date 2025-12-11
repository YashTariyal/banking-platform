# Support Service

The Support Service provides back-office case management and manual override capabilities for banking operations, enabling support staff to handle customer inquiries, disputes, and administrative overrides.

## Overview

The Support Service provides:
- **Support Case Management**: Create, assign, track, and resolve customer support cases
- **Case Workflow**: Manage cases through lifecycle (OPEN → ASSIGNED → IN_PROGRESS → RESOLVED/CLOSED)
- **Manual Overrides**: Request, approve, and manage manual overrides for account limits, transaction approvals, fee waivers, etc.
- **Override Workflow**: PENDING → APPROVED/REJECTED workflow with expiration support
- **Kafka Integration**: Publishes case and override lifecycle events

## Domain Model

### Support Case Entity

- **id**: UUID primary key
- **caseNumber**: Unique case number (CASE + 10 digits)
- **caseType**: Type of case (ACCOUNT_INQUIRY, TRANSACTION_DISPUTE, FRAUD_REPORT, etc.)
- **priority**: Case priority (LOW, MEDIUM, HIGH, URGENT)
- **status**: Case status (OPEN, ASSIGNED, IN_PROGRESS, PENDING_CUSTOMER, RESOLVED, CLOSED, ESCALATED, CANCELLED)
- **customerId**: Associated customer UUID
- **accountId**: Associated account UUID (optional)
- **title**: Case title
- **description**: Case description
- **assignedTo**: UUID of assigned support agent
- **createdBy**: UUID of user who created the case
- **resolvedBy**: UUID of user who resolved the case
- **resolvedAt**: Resolution timestamp
- **resolutionNotes**: Resolution notes
- **dueDate**: Case due date
- **createdAt**, **updatedAt**: Audit timestamps

### Manual Override Entity

- **id**: UUID primary key
- **overrideType**: Type of override (ACCOUNT_LIMIT, TRANSACTION_LIMIT, FEE_WAIVER, etc.)
- **status**: Override status (PENDING, APPROVED, REJECTED, EXPIRED, REVOKED)
- **customerId**: Associated customer UUID
- **accountId**: Associated account UUID
- **entityId**: Associated entity UUID (transaction, loan, etc.)
- **requestedBy**: UUID of user who requested the override
- **approvedBy**: UUID of user who approved the override
- **rejectedBy**: UUID of user who rejected the override
- **reason**: Reason for override request
- **overrideValue**: Override value (JSON string or text)
- **amount**: Override amount (if applicable)
- **currency**: Currency code (ISO 4217)
- **expiresAt**: Override expiration timestamp
- **approvedAt**: Approval timestamp
- **rejectedAt**: Rejection timestamp
- **rejectionReason**: Rejection reason
- **createdAt**, **updatedAt**: Audit timestamps

### Case Types

- **ACCOUNT_INQUIRY**: Account-related inquiries
- **TRANSACTION_DISPUTE**: Transaction disputes
- **FRAUD_REPORT**: Fraud reports
- **ACCOUNT_LIMIT_CHANGE**: Account limit change requests
- **FEE_DISPUTE**: Fee disputes
- **CARD_ISSUE**: Card-related issues
- **LOAN_INQUIRY**: Loan inquiries
- **PAYMENT_ISSUE**: Payment issues
- **GENERAL_INQUIRY**: General inquiries
- **COMPLAINT**: Customer complaints
- **OTHER**: Other types

### Case Priorities

- **LOW**: Low priority cases
- **MEDIUM**: Medium priority cases
- **HIGH**: High priority cases
- **URGENT**: Urgent cases requiring immediate attention

### Case Statuses

- **OPEN**: New case, not yet assigned
- **ASSIGNED**: Case assigned to a support agent
- **IN_PROGRESS**: Case is being worked on
- **PENDING_CUSTOMER**: Waiting for customer response
- **RESOLVED**: Case resolved
- **CLOSED**: Case closed
- **ESCALATED**: Case escalated to higher level
- **CANCELLED**: Case cancelled

### Override Types

- **ACCOUNT_LIMIT**: Account limit overrides
- **TRANSACTION_LIMIT**: Transaction limit overrides
- **DAILY_LIMIT**: Daily limit overrides
- **WITHDRAWAL_LIMIT**: Withdrawal limit overrides
- **TRANSFER_LIMIT**: Transfer limit overrides
- **FEE_WAIVER**: Fee waiver overrides
- **INTEREST_RATE**: Interest rate overrides
- **LOAN_APPROVAL**: Loan approval overrides
- **PAYMENT_APPROVAL**: Payment approval overrides
- **CARD_APPROVAL**: Card approval overrides
- **OTHER**: Other override types

### Override Statuses

- **PENDING**: Override request pending approval
- **APPROVED**: Override approved
- **REJECTED**: Override rejected
- **EXPIRED**: Override expired
- **REVOKED**: Override revoked

## API Endpoints

### Create Support Case

```http
POST /api/support/cases?createdBy={userId}
Content-Type: application/json

{
  "caseType": "ACCOUNT_INQUIRY",
  "priority": "HIGH",
  "customerId": "uuid",
  "accountId": "uuid",
  "title": "Account inquiry",
  "description": "Customer inquiry about account balance",
  "dueDate": "2024-01-15T00:00:00Z"
}
```

**Response:**
```json
{
  "id": "uuid",
  "caseNumber": "CASE1234567890",
  "caseType": "ACCOUNT_INQUIRY",
  "priority": "HIGH",
  "status": "OPEN",
  "customerId": "uuid",
  "accountId": "uuid",
  "title": "Account inquiry",
  "description": "Customer inquiry about account balance",
  "assignedTo": null,
  "createdBy": "uuid",
  "resolvedBy": null,
  "resolvedAt": null,
  "resolutionNotes": null,
  "dueDate": "2024-01-15T00:00:00Z",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Get Support Case

```http
GET /api/support/cases/{id}
```

### Get Case by Number

```http
GET /api/support/cases/number/{caseNumber}
```

### List Support Cases

```http
GET /api/support/cases?status=OPEN&priority=HIGH&customerId={uuid}&page=0&size=20
```

**Query Parameters:**
- `status`: Filter by case status
- `priority`: Filter by case priority
- `caseType`: Filter by case type
- `customerId`: Filter by customer ID
- `assignedTo`: Filter by assigned agent ID
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

### Assign Case

```http
PUT /api/support/cases/{id}/assign?assignedTo={agentId}
```

### Update Case Status

```http
PUT /api/support/cases/{id}/status?updatedBy={userId}
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

### Resolve Case

```http
PUT /api/support/cases/{id}/resolve?resolvedBy={userId}
Content-Type: application/json

{
  "resolutionNotes": "Issue resolved by contacting customer"
}
```

### Create Manual Override

```http
POST /api/support/overrides?requestedBy={userId}
Content-Type: application/json

{
  "overrideType": "ACCOUNT_LIMIT",
  "customerId": "uuid",
  "accountId": "uuid",
  "reason": "Customer request for higher limit",
  "overrideValue": "50000",
  "amount": 50000.00,
  "currency": "USD",
  "expiresAt": "2024-12-31T23:59:59Z"
}
```

### Get Manual Override

```http
GET /api/support/overrides/{id}
```

### List Manual Overrides

```http
GET /api/support/overrides?status=PENDING&overrideType=ACCOUNT_LIMIT&customerId={uuid}&page=0&size=20
```

**Query Parameters:**
- `status`: Filter by override status
- `overrideType`: Filter by override type
- `customerId`: Filter by customer ID
- `accountId`: Filter by account ID
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

### Get Active Overrides

```http
GET /api/support/overrides/active/{customerId}/{overrideType}
```

### Approve Override

```http
PUT /api/support/overrides/{id}/approve?approvedBy={userId}
```

### Reject Override

```http
PUT /api/support/overrides/{id}/reject?rejectedBy={userId}
Content-Type: application/json

{
  "rejectionReason": "Insufficient justification"
}
```

### Revoke Override

```http
PUT /api/support/overrides/{id}/revoke?revokedBy={userId}
```

## Database Schema

### support_cases Table

```sql
CREATE TABLE support_cases (
    id UUID PRIMARY KEY,
    case_number VARCHAR(20) UNIQUE NOT NULL,
    case_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    customer_id UUID NOT NULL,
    account_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to UUID,
    created_by UUID NOT NULL,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,
    due_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**Indexes:**
- `idx_support_cases_case_number`
- `idx_support_cases_status`
- `idx_support_cases_priority`
- `idx_support_cases_case_type`
- `idx_support_cases_customer_id`
- `idx_support_cases_assigned_to`
- `idx_support_cases_created_at`

### manual_overrides Table

```sql
CREATE TABLE manual_overrides (
    id UUID PRIMARY KEY,
    override_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    customer_id UUID,
    account_id UUID,
    entity_id UUID,
    requested_by UUID NOT NULL,
    approved_by UUID,
    rejected_by UUID,
    reason TEXT NOT NULL,
    override_value TEXT,
    amount NUMERIC(19, 2),
    currency VARCHAR(3),
    expires_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**Indexes:**
- `idx_manual_overrides_status`
- `idx_manual_overrides_override_type`
- `idx_manual_overrides_customer_id`
- `idx_manual_overrides_account_id`
- `idx_manual_overrides_expires_at`
- `idx_manual_overrides_created_at`

## Kafka Integration

The service publishes events to the `support-events` topic:

### Event Types

- **SUPPORT_CASE_CREATED**: Published when a new support case is created
- **SUPPORT_CASE_UPDATED**: Published when a case is updated (assigned, status changed)
- **SUPPORT_CASE_RESOLVED**: Published when a case is resolved
- **MANUAL_OVERRIDE_CREATED**: Published when a new override is requested
- **MANUAL_OVERRIDE_APPROVED**: Published when an override is approved
- **MANUAL_OVERRIDE_REJECTED**: Published when an override is rejected
- **MANUAL_OVERRIDE_REVOKED**: Published when an override is revoked

### Event Structure

```json
{
  "eventType": "SUPPORT_CASE_CREATED",
  "caseId": "uuid",
  "overrideId": null,
  "caseNumber": "CASE1234567890",
  "caseType": "ACCOUNT_INQUIRY",
  "priority": "HIGH",
  "caseStatus": "OPEN",
  "overrideType": null,
  "overrideStatus": null,
  "customerId": "uuid",
  "accountId": "uuid",
  "assignedTo": null,
  "createdBy": "uuid",
  "requestedBy": null,
  "approvedBy": null,
  "amount": null,
  "currency": null,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Service Configuration

**Port**: 8089

**Database**: PostgreSQL (`support_service`)

**Kafka Topic**: `support-events`

## Testing

The service includes comprehensive test coverage:

- **CaseNumberGeneratorTest**: 2 tests covering case number generation
- **SupportCaseServiceTest**: 6 tests covering case management
- **ManualOverrideServiceTest**: 7 tests covering override management
- **SupportCaseControllerTest**: 3 tests covering REST endpoints
- **ManualOverrideControllerTest**: 3 tests covering REST endpoints

**Total**: 21 tests

## Usage Examples

### Create Support Case

```java
SupportCase supportCase = supportCaseService.createCase(
    CaseType.ACCOUNT_INQUIRY,
    CasePriority.HIGH,
    customerId,
    accountId,
    "Account inquiry",
    "Customer inquiry about account balance",
    createdBy,
    dueDate
);
```

### Assign Case

```java
SupportCase supportCase = supportCaseService.assignCase(
    caseId,
    agentId
);
```

### Resolve Case

```java
SupportCase supportCase = supportCaseService.resolveCase(
    caseId,
    "Issue resolved",
    resolvedBy
);
```

### Create Manual Override

```java
ManualOverride override = manualOverrideService.createOverride(
    OverrideType.ACCOUNT_LIMIT,
    customerId,
    accountId,
    null,
    "Customer request",
    "50000",
    new BigDecimal("50000"),
    "USD",
    expiresAt,
    requestedBy
);
```

### Approve Override

```java
ManualOverride override = manualOverrideService.approveOverride(
    overrideId,
    approvedBy
);
```

## Integration Points

The Support Service can be integrated with:

- **Customer Service**: Retrieve customer information for cases
- **Account Service**: Apply account limit overrides
- **Transaction Service**: Apply transaction limit overrides
- **Payment Service**: Apply payment approval overrides
- **Loan Service**: Apply loan approval overrides
- **Card Service**: Apply card approval overrides

## Future Enhancements

- Case escalation workflows
- SLA tracking and alerts
- Automated case routing based on type/priority
- Integration with ticketing systems
- Case templates and knowledge base
- Override expiration monitoring and notifications
- Audit trail for all case and override actions
- Reporting and analytics dashboards

