# Account Service

## Overview
The account service manages customer accounts, exposes REST APIs for lifecycle operations, and publishes Kafka events (`accounts.account-created`, `accounts.account-updated`) with the schema defined in `services/account-service/src/main/resources/schemas/account-event.schema.json`.

## Running Locally
1. Start infra (Kafka + Postgres):
   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/infrastructure
   docker compose up -d
   ```
2. Launch the service:
   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/services/account-service
   mvn spring-boot:run
   ```
3. Access docs/health:
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **API Docs (JSON)**: http://localhost:8080/api-docs
   - **Actuator Health**: http://localhost:8080/actuator/health
   
   Note: If the server port is set to 0 (random port), check the console output for the actual port number.

## REST Endpoints
| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/accounts` | Create an account (requires `customerId`, `type`, `currency`, `initialDeposit`). |
| GET | `/api/accounts/{id}` | Retrieve an account by ID. |
| GET | `/api/accounts?customerId=&page=&size=` | List accounts with optional customer filter and pagination. |
| PUT | `/api/accounts/{id}` | Update account details (`type`, `currency`). |
| PUT | `/api/accounts/{id}/status` | Update account status (`ACTIVE/SUSPENDED/CLOSED`). |
| GET | `/api/accounts/{id}/balance` | Get account balance information. |
| POST | `/api/accounts/{id}/transactions` | Apply credit/debit with validation for sufficient funds. Body must include a unique `referenceId` (UUID) per account to guarantee idempotency. Transactions are blocked on `CLOSED` or `SUSPENDED` accounts. |
| GET | `/api/accounts/{id}/transactions?page=&size=` | Get transaction history for an account with pagination. |
| DELETE | `/api/accounts/{id}` | Delete an account. |

### Savings Goals & Auto-Sweep
| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/accounts/{id}/goals` | Create a named savings goal with optional auto-sweep configuration (cadence + contribution amount). |
| GET | `/api/accounts/{id}/goals?page=&size=` | List goals for an account. |
| GET | `/api/accounts/{id}/goals/{goalId}` | Fetch a specific goal. |
| PUT | `/api/accounts/{id}/goals/{goalId}` | Update goal details or toggle auto-sweep. |
| POST | `/api/accounts/{id}/goals/{goalId}/contributions` | Apply a manual contribution. The service debits the account and updates goal progress. |

Auto-sweep runs daily at 01:15 UTC by default. It debits excess balances above the configured buffer (`account.goals.auto-sweep.min-balance-buffer`) and uses deterministic reference IDs so retries remain idempotent. Configure cadence defaults and sweep limits under the `account.goals` section in `application.yml`.

## Testing
```bash
cd /Users/tariyalji/gitbot/banking-platform/services/account-service
mvn test
```
The suite includes unit tests (service + controller) and repository integration tests powered by Testcontainers/PostgreSQL so the schema mirrors production.

## Observability
- Micrometer metrics: `accounts.created`, `accounts.updated`, `accounts.transactions.credit`, `accounts.transactions.debit`.
- Distributed tracing enabled (Micrometer tracing bridge / Zipkin reporter).
- Request logging via `CommonsRequestLoggingFilter`.
- Event monitoring: every Kafka publish is captured in the `event_audit_logs` table (direction, status, payload snapshot, partition/offset). The data is written via an AOP interceptor around `KafkaTemplate.send`, so failures and retries are visible without touching producer code.

## Concurrency & Idempotency
- Each account row is protected with optimistic locking via a `version` column to prevent lost updates.
- Transaction requests require a `referenceId`; duplicate submissions with the same reference are ignored gracefully.
- All applied transactions are persisted to the `account_transactions` log table, which can be queried for audits or replay protection.

## Event Schema
Producers serialize `AccountEvent` instances as JSON (see schema file). Consumers should validate against the JSON schema or mirror the record definition to ensure compatibility.

