# Transaction Service

The Transaction Service provides transaction orchestration and lifecycle management for banking operations, coordinating transactions between accounts and managing transaction workflows.

## Overview

The Transaction Service provides:
- **Transaction Creation**: Create transactions with idempotency support via reference IDs
- **Transaction Lifecycle**: Manage transaction status (PENDING → PROCESSING → COMPLETED/FAILED)
- **Transaction Types**: Support for deposits, withdrawals, transfers, payments, fees, interest, refunds, reversals, and adjustments
- **Transaction Reversal**: Reverse completed transactions
- **Transaction Cancellation**: Cancel pending transactions
- **Kafka Integration**: Publishes transaction lifecycle events for other services to consume

## Domain Model

### Transaction Entity

- **id**: UUID primary key
- **referenceId**: Unique reference identifier (idempotency key)
- **transactionType**: Type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, etc.)
- **status**: Transaction status (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED)
- **fromAccountId**: Source account UUID (optional)
- **toAccountId**: Destination account UUID (optional)
- **customerId**: Associated customer UUID
- **amount**: Transaction amount
- **currency**: Currency code (ISO 4217)
- **description**: Transaction description
- **failureReason**: Failure reason if transaction failed
- **initiatedAt**: Transaction initiation timestamp
- **completedAt**: Transaction completion timestamp
- **createdAt**, **updatedAt**: Audit timestamps

### Transaction Types

- **DEPOSIT**: Money deposited into an account
- **WITHDRAWAL**: Money withdrawn from an account
- **TRANSFER**: Transfer between accounts
- **PAYMENT**: Payment transaction
- **FEE**: Fee transaction
- **INTEREST**: Interest transaction
- **REFUND**: Refund transaction
- **REVERSAL**: Transaction reversal
- **ADJUSTMENT**: Account adjustment

### Transaction Statuses

- **PENDING**: Transaction created but not yet processed
- **PROCESSING**: Transaction is being processed
- **COMPLETED**: Transaction completed successfully
- **FAILED**: Transaction failed
- **CANCELLED**: Transaction cancelled
- **REVERSED**: Transaction reversed

## API Endpoints

### Create Transaction

```http
POST /api/transactions
Content-Type: application/json

{
  "referenceId": "REF-123",
  "transactionType": "DEPOSIT",
  "customerId": "uuid",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 1000.00,
  "currency": "USD",
  "description": "Deposit transaction"
}
```

**Response:**
```json
{
  "id": "uuid",
  "referenceId": "REF-123",
  "transactionType": "DEPOSIT",
  "status": "PENDING",
  "customerId": "uuid",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 1000.00,
  "currency": "USD",
  "description": "Deposit transaction",
  "failureReason": null,
  "initiatedAt": "2024-01-01T00:00:00Z",
  "completedAt": null,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Get Transaction

```http
GET /api/transactions/{id}
```

### Get Transaction by Reference ID

```http
GET /api/transactions/reference/{referenceId}
```

### List Transactions

```http
GET /api/transactions?status=COMPLETED&transactionType=DEPOSIT&customerId={uuid}&accountId={uuid}&page=0&size=20
```

**Query Parameters:**
- `status`: Filter by transaction status
- `transactionType`: Filter by transaction type
- `customerId`: Filter by customer ID
- `accountId`: Filter by account ID (from or to)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

### Process Transaction

```http
PUT /api/transactions/{id}/process
```

### Complete Transaction

```http
PUT /api/transactions/{id}/complete
```

### Fail Transaction

```http
PUT /api/transactions/{id}/fail
Content-Type: application/json

{
  "failureReason": "Insufficient funds"
}
```

### Cancel Transaction

```http
PUT /api/transactions/{id}/cancel
```

### Reverse Transaction

```http
PUT /api/transactions/{id}/reverse
Content-Type: application/json

{
  "reason": "Customer request"
}
```

## Database Schema

### transactions Table

```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(100) UNIQUE NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    from_account_id UUID,
    to_account_id UUID,
    customer_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(500),
    failure_reason VARCHAR(1000),
    initiated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**Indexes:**
- `idx_transactions_reference_id`
- `idx_transactions_status`
- `idx_transactions_transaction_type`
- `idx_transactions_customer_id`
- `idx_transactions_from_account_id`
- `idx_transactions_to_account_id`
- `idx_transactions_created_at`

## Kafka Integration

The service publishes events to the `transaction-events` topic:

### Event Types

- **TRANSACTION_INITIATED**: Published when a new transaction is created
- **TRANSACTION_PROCESSING**: Published when transaction processing starts
- **TRANSACTION_COMPLETED**: Published when transaction completes successfully
- **TRANSACTION_FAILED**: Published when transaction fails
- **TRANSACTION_CANCELLED**: Published when transaction is cancelled
- **TRANSACTION_REVERSED**: Published when transaction is reversed

### Event Structure

```json
{
  "eventType": "TRANSACTION_COMPLETED",
  "transactionId": "uuid",
  "referenceId": "REF-123",
  "transactionType": "DEPOSIT",
  "status": "COMPLETED",
  "customerId": "uuid",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 1000.00,
  "currency": "USD",
  "description": "Deposit transaction",
  "failureReason": null,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Service Configuration

**Port**: 8090

**Database**: PostgreSQL (`transaction_service`)

**Kafka Topic**: `transaction-events`

## Testing

The service includes comprehensive test coverage:

- **TransactionServiceTest**: 7 tests covering transaction lifecycle management
- **TransactionControllerTest**: 4 tests covering REST endpoints

**Total**: 11 tests

## Usage Examples

### Create Transaction

```java
Transaction transaction = transactionService.createTransaction(
    "REF-123",
    TransactionType.DEPOSIT,
    customerId,
    null,
    toAccountId,
    new BigDecimal("1000"),
    "USD",
    "Deposit transaction"
);
```

### Process Transaction

```java
Transaction transaction = transactionService.processTransaction(transactionId);
```

### Complete Transaction

```java
Transaction transaction = transactionService.completeTransaction(transactionId);
```

### Reverse Transaction

```java
Transaction transaction = transactionService.reverseTransaction(
    transactionId,
    "Customer request"
);
```

## Integration Points

The Transaction Service integrates with:

- **Account Service**: Updates account balances for deposits/withdrawals/transfers
- **Ledger Service**: Posts journal entries for transactions
- **Payment Service**: Coordinates payment transactions
- **Compliance Service**: Consumes transaction events for AML monitoring
- **Risk Service**: Consumes transaction events for risk assessment

## Idempotency

Transactions support idempotency via the `referenceId` field. If a transaction with the same `referenceId` already exists, the service will return a conflict error, preventing duplicate transactions.

## Transaction Lifecycle

1. **PENDING**: Transaction created, awaiting processing
2. **PROCESSING**: Transaction is being processed (e.g., account balance updates, ledger postings)
3. **COMPLETED**: Transaction completed successfully
4. **FAILED**: Transaction failed (e.g., insufficient funds, validation errors)
5. **CANCELLED**: Transaction cancelled before processing
6. **REVERSED**: Completed transaction reversed

## Future Enhancements

- Saga pattern implementation for distributed transactions
- Transaction batching and bulk operations
- Transaction scheduling and recurring transactions
- Multi-currency transaction support
- Transaction fees calculation
- Transaction limits and validation rules
- Integration with external payment processors
- Real-time transaction monitoring and alerts

