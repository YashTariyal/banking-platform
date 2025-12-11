# Payment Service

The Payment Service handles external payment rail integrations for processing payments through various channels including ACH, Wire, SWIFT, FedWire, RTP, and card networks.

## Overview

The Payment Service provides:
- **Payment Initiation**: Create payments through different payment rails
- **Payment Processing**: Process payments through external rails
- **Payment Lifecycle Management**: Track payment status (PENDING → PROCESSING → COMPLETED/FAILED)
- **Payment Cancellation**: Cancel pending payments
- **Payment Reversal**: Reverse completed payments
- **Kafka Integration**: Publishes payment lifecycle events

## Domain Model

### Payment Entity

- **id**: UUID primary key
- **referenceId**: Unique reference identifier (idempotency key)
- **rail**: Payment rail (ACH, WIRE, SWIFT, FEDWIRE, RTP, CARD_NETWORK)
- **direction**: INBOUND or OUTBOUND
- **status**: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED
- **fromAccountId**: Source account UUID
- **toAccountId**: Destination account UUID (for internal transfers)
- **toExternalAccount**: External account number (for external payments)
- **toExternalRouting**: External routing number
- **toExternalBankName**: External bank name
- **amount**: Payment amount
- **currency**: Currency code (ISO 4217)
- **description**: Payment description
- **failureReason**: Failure reason if payment failed
- **externalReference**: External payment reference from rail provider
- **initiatedAt**: Payment initiation timestamp
- **completedAt**: Payment completion timestamp

### Payment Rails

- **ACH**: Automated Clearing House
- **WIRE**: Wire transfer
- **SWIFT**: SWIFT network
- **FEDWIRE**: Federal Reserve Wire Network
- **RTP**: Real-Time Payments
- **CARD_NETWORK**: Card network payments

## API Endpoints

### Create Payment

```http
POST /api/payments
Content-Type: application/json

{
  "referenceId": "REF-123",
  "rail": "ACH",
  "direction": "OUTBOUND",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "toExternalAccount": "1234567890",
  "toExternalRouting": "021000021",
  "toExternalBankName": "Bank Name",
  "amount": 100.00,
  "currency": "USD",
  "description": "Payment description"
}
```

**Response**: 201 Created with payment details

### Get Payment

```http
GET /api/payments/{id}
```

**Response**: Payment details

### Get Payment by Reference ID

```http
GET /api/payments/reference/{referenceId}
```

**Response**: Payment details

### Get Payments by Account

```http
GET /api/payments/account/{accountId}?page=0&size=20
```

**Response**: Paginated list of payments

### Get Payments by Status

```http
GET /api/payments?status=PENDING&page=0&size=20
```

**Response**: Paginated list of payments

### Process Payment

```http
PUT /api/payments/{id}/process
Content-Type: application/json

{
  "externalReference": "EXT-REF-123"
}
```

**Response**: Updated payment with PROCESSING status

### Complete Payment

```http
PUT /api/payments/{id}/complete?externalReference=EXT-REF-123
```

**Response**: Updated payment with COMPLETED status

### Fail Payment

```http
PUT /api/payments/{id}/fail
Content-Type: application/json

{
  "failureReason": "Insufficient funds"
}
```

**Response**: Updated payment with FAILED status

### Cancel Payment

```http
PUT /api/payments/{id}/cancel
```

**Response**: Updated payment with CANCELLED status

## Payment Lifecycle

1. **PENDING**: Payment created, awaiting processing
2. **PROCESSING**: Payment submitted to external rail
3. **COMPLETED**: Payment successfully processed
4. **FAILED**: Payment failed (with failure reason)
5. **CANCELLED**: Payment cancelled before processing
6. **REVERSED**: Completed payment reversed

## Kafka Events

The service publishes the following events to `payment-events` topic:

- **PAYMENT_INITIATED**: Payment created
- **PAYMENT_PROCESSING**: Payment submitted to rail
- **PAYMENT_COMPLETED**: Payment successfully completed
- **PAYMENT_FAILED**: Payment failed
- **PAYMENT_CANCELLED**: Payment cancelled

## Database Schema

### payments table

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL UNIQUE,
    rail VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    from_account_id UUID NOT NULL,
    to_account_id UUID,
    to_external_account VARCHAR(255),
    to_external_routing VARCHAR(255),
    to_external_bank_name VARCHAR(255),
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    external_reference VARCHAR(255),
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Configuration

- **Port**: 8087
- **Database**: `payment_service` (PostgreSQL)
- **Kafka Topic**: `payment-events`

## Testing

Run tests with:

```bash
cd services/payment-service
mvn test
```

## Integration Notes

- Payments are idempotent via `referenceId`
- External rail integration points are simulated (replace with actual rail APIs)
- Payment processing can be async (implement async workers for production)
- Consider implementing retry logic for failed payments
- Add webhook endpoints for external rail callbacks

