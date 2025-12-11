# Risk Service

The Risk Service provides comprehensive risk assessment and alerting capabilities for banking operations, including transaction monitoring, fraud detection, and risk scoring.

## Overview

The Risk Service provides:
- **Risk Assessment**: Evaluate risk for transactions, accounts, customers, payments, cards, and loans
- **Risk Scoring**: Calculate risk scores based on multiple factors (amount, velocity, location, time patterns)
- **Risk Alerts**: Automatically generate alerts for medium-to-high risk assessments
- **Alert Management**: Review, resolve, and manage risk alerts
- **Kafka Integration**: Publishes risk assessment and alert events

## Domain Model

### Risk Assessment Entity

- **id**: UUID primary key
- **riskType**: Type of risk assessment (TRANSACTION, ACCOUNT, CUSTOMER, PAYMENT, CARD, LOAN)
- **entityId**: UUID of the entity being assessed
- **riskLevel**: Risk level (LOW, MEDIUM, HIGH, CRITICAL)
- **riskScore**: Numeric risk score (0-100+)
- **customerId**: Associated customer UUID
- **accountId**: Associated account UUID
- **amount**: Transaction/entity amount
- **currency**: Currency code (ISO 4217)
- **riskFactors**: Comma-separated list of risk factors identified
- **description**: Assessment description
- **assessedAt**: Assessment timestamp
- **createdAt**, **updatedAt**: Audit timestamps

### Risk Alert Entity

- **id**: UUID primary key
- **riskAssessmentId**: Foreign key to risk assessment
- **status**: Alert status (OPEN, UNDER_REVIEW, RESOLVED, FALSE_POSITIVE, ESCALATED)
- **riskLevel**: Risk level (LOW, MEDIUM, HIGH, CRITICAL)
- **riskScore**: Numeric risk score
- **customerId**: Associated customer UUID
- **accountId**: Associated account UUID
- **title**: Alert title
- **description**: Alert description
- **reviewedBy**: UUID of reviewer
- **reviewedAt**: Review timestamp
- **resolutionNotes**: Resolution notes
- **createdAt**, **updatedAt**: Audit timestamps

### Risk Types

- **TRANSACTION**: Transaction risk assessment
- **ACCOUNT**: Account-level risk assessment
- **CUSTOMER**: Customer-level risk assessment
- **PAYMENT**: Payment risk assessment
- **CARD**: Card transaction risk assessment
- **LOAN**: Loan risk assessment

### Risk Levels

- **LOW**: Risk score < 30
- **MEDIUM**: Risk score 30-59
- **HIGH**: Risk score 60-79
- **CRITICAL**: Risk score ≥ 80

### Alert Status

- **OPEN**: New alert, not yet reviewed
- **UNDER_REVIEW**: Alert is being reviewed
- **RESOLVED**: Alert resolved
- **FALSE_POSITIVE**: Alert marked as false positive
- **ESCALATED**: Alert escalated for further investigation

## API Endpoints

### Assess Risk

```http
POST /api/risk/assessments
Content-Type: application/json

{
  "riskType": "TRANSACTION",
  "entityId": "uuid",
  "customerId": "uuid",
  "accountId": "uuid",
  "amount": 10000.00,
  "currency": "USD",
  "description": "Large transaction"
}
```

**Response:**
```json
{
  "id": "uuid",
  "riskType": "TRANSACTION",
  "entityId": "uuid",
  "riskLevel": "HIGH",
  "riskScore": 75,
  "customerId": "uuid",
  "accountId": "uuid",
  "amount": 10000.00,
  "currency": "USD",
  "riskFactors": "Large transaction amount, High transaction velocity",
  "description": "Large transaction",
  "assessedAt": "2024-01-01T00:00:00Z",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Get Risk Assessment

```http
GET /api/risk/assessments/{id}
```

### List Risk Assessments

```http
GET /api/risk/assessments?riskType=TRANSACTION&riskLevel=HIGH&page=0&size=20
```

**Query Parameters:**
- `riskType`: Filter by risk type
- `riskLevel`: Filter by risk level
- `customerId`: Filter by customer ID
- `accountId`: Filter by account ID
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

### Get Assessments by Entity

```http
GET /api/risk/assessments/entity/{riskType}/{entityId}
```

### Get Risk Alert

```http
GET /api/risk/alerts/{id}
```

### List Risk Alerts

```http
GET /api/risk/alerts?status=OPEN&riskLevel=HIGH&page=0&size=20
```

**Query Parameters:**
- `status`: Filter by alert status
- `riskLevel`: Filter by risk level
- `customerId`: Filter by customer ID
- `accountId`: Filter by account ID
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

### Update Alert Status

```http
PUT /api/risk/alerts/{id}/status
Content-Type: application/json

{
  "status": "RESOLVED",
  "reviewedBy": "uuid",
  "resolutionNotes": "Resolved as false positive"
}
```

## Risk Scoring Algorithm

The risk scoring service calculates risk scores based on multiple factors:

### Amount-Based Factors

| Factor | Points | Threshold |
|--------|--------|-----------|
| Large Transaction (≥$10,000) | +25 | - |
| Moderate Transaction ($5,000-$9,999) | +15 | - |
| Round Number Pattern | +10 | - |

### Velocity-Based Factors

| Factor | Points | Threshold |
|--------|--------|-----------|
| High Transaction Velocity (>10 transactions) | +20 | - |
| High Cumulative Amount (>$50,000) | +15 | - |

### Context-Based Factors

| Factor | Points | Threshold |
|--------|--------|-----------|
| New Account | +10 | - |
| Unusual Location | +20 | - |
| Unusual Time | +15 | - |

### Risk Level Thresholds

- **LOW**: Score < 30
- **MEDIUM**: Score 30-59
- **HIGH**: Score 60-79
- **CRITICAL**: Score ≥ 80

## Database Schema

### risk_assessments Table

```sql
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY,
    risk_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    risk_score INTEGER NOT NULL,
    customer_id UUID,
    account_id UUID,
    amount NUMERIC(19, 2),
    currency VARCHAR(3),
    risk_factors TEXT,
    description VARCHAR(500),
    assessed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**Indexes:**
- `idx_risk_assessments_risk_type`
- `idx_risk_assessments_risk_level`
- `idx_risk_assessments_customer_id`
- `idx_risk_assessments_account_id`
- `idx_risk_assessments_entity_id`
- `idx_risk_assessments_assessed_at`

### risk_alerts Table

```sql
CREATE TABLE risk_alerts (
    id UUID PRIMARY KEY,
    risk_assessment_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    risk_score INTEGER NOT NULL,
    customer_id UUID,
    account_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_risk_alerts_assessment FOREIGN KEY (risk_assessment_id) 
        REFERENCES risk_assessments(id) ON DELETE CASCADE
);
```

**Indexes:**
- `idx_risk_alerts_status`
- `idx_risk_alerts_risk_level`
- `idx_risk_alerts_customer_id`
- `idx_risk_alerts_account_id`
- `idx_risk_alerts_assessment_id`
- `idx_risk_alerts_created_at`

## Kafka Integration

The service publishes events to the `risk-events` topic:

### Event Types

- **RISK_ASSESSMENT_CREATED**: Published when a new risk assessment is created
- **RISK_ALERT_CREATED**: Published when a new risk alert is created (for MEDIUM+ risk)
- **RISK_ALERT_UPDATED**: Published when an alert status is updated

### Event Structure

```json
{
  "eventType": "RISK_ASSESSMENT_CREATED",
  "assessmentId": "uuid",
  "alertId": null,
  "riskType": "TRANSACTION",
  "entityId": "uuid",
  "customerId": "uuid",
  "accountId": "uuid",
  "riskLevel": "HIGH",
  "riskScore": 75,
  "amount": 10000.00,
  "currency": "USD",
  "riskFactors": "Large transaction amount",
  "alertStatus": null,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Service Configuration

**Port**: 8088

**Database**: PostgreSQL (`risk_service`)

**Kafka Topic**: `risk-events`

## Testing

The service includes comprehensive test coverage:

- **RiskScoringServiceTest**: 7 tests covering risk scoring logic
- **RiskAssessmentServiceTest**: 6 tests covering assessment creation and retrieval
- **RiskAlertServiceTest**: 6 tests covering alert management
- **RiskAssessmentControllerTest**: 3 tests covering REST endpoints
- **RiskAlertControllerTest**: 3 tests covering REST endpoints

**Total**: 25 tests

## Usage Examples

### Assess Transaction Risk

```java
RiskAssessment assessment = riskAssessmentService.assessRisk(
    RiskType.TRANSACTION,
    transactionId,
    customerId,
    accountId,
    new BigDecimal("15000"),
    "USD",
    "Large transaction",
    context
);
```

### Get High-Risk Alerts

```java
Page<RiskAlert> alerts = riskAlertService.getAlertsByRiskLevel(
    RiskLevel.HIGH,
    PageRequest.of(0, 20)
);
```

### Resolve Alert

```java
RiskAlert alert = riskAlertService.updateAlertStatus(
    alertId,
    AlertStatus.RESOLVED,
    reviewerId,
    "Resolved as false positive"
);
```

## Integration Points

The Risk Service can be integrated with:

- **Transaction Service**: Assess transaction risk
- **Payment Service**: Assess payment risk
- **Card Service**: Assess card transaction risk
- **Loan Service**: Assess loan risk
- **Compliance Service**: Share risk assessments for AML/compliance checks
- **Customer Service**: Assess customer-level risk

## Future Enhancements

- Machine learning-based risk scoring
- Real-time risk monitoring dashboards
- Automated alert routing based on risk level
- Integration with external fraud detection services
- Risk pattern detection and learning
- Custom risk rules configuration

