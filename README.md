# Banking Platform (Spring Boot + Kafka)

This monorepo scaffolds twelve domain-driven banking microservices built with Java 21, Spring Boot 3, Kafka, Hibernate, and PostgreSQL/MySQL drivers. Each service runs independently but shares conventions for messaging, persistence, and observability.

## Tech Stack
- Java 21, Spring Boot 3.3
- Spring Web, Data JPA (Hibernate), Validation
- Spring for Apache Kafka
- PostgreSQL (primary) + MySQL connector for cross-RDBMS support
- Actuator for health/metrics
- Maven multi-module build

## Modules
| Service | Responsibility |
| --- | --- |
| Identity | Auth, sessions, MFA
| Customer | PII, contact data, preferences
| KYC | Onboarding workflows, screening
| Account | Account catalog, lifecycle, limits
| Ledger | Double-entry postings, balances
| Transaction | Payment orchestration & sagas
| Payment | External rail integrations
| Card | Card issuance, auth handling
| Loan | Applications, schedules, repayments
| Risk | Fraud/risk scoring, alerts
| Compliance | Regulatory reports, AML
| Support | Back-office cases, manual overrides

Each module currently exposes a minimal Spring Boot bootstrap class, Kafka + datasource configs, and a dedicated Maven `pom.xml`. Extend each service with controllers, entities, repositories, and messaging adapters as requirements evolve.

Service-specific runbooks and API docs live under `docs/`:
- [Account Service](docs/account-service.md)
- [Compliance Service](docs/compliance-service.md)
- [Customer Service](docs/customer-service.md)

## Building & Running
```bash
mvn -version    # ensure Maven + Java 21
mvn clean verify
```
To run an individual service (e.g., Account):
```bash
cd services/account-service
mvn spring-boot:run
```
Override datasource/Kafka settings via environment variables or `application-local.yml`.

## Local Infrastructure (Kafka + Databases)
```bash
cd infrastructure
cp .env.example .env   # optional override of ports/credentials
docker compose up -d
```
Services exposed locally:
- Kafka broker `localhost:9092`, Schema Registry `http://localhost:8081`, Kafka UI `http://localhost:8085`
- PostgreSQL `localhost:5432` seeded with one database/user per service (password `changeMe!`)
- MySQL `localhost:3306` (root password `root`) for cross-database testing

Bring everything down with:
```bash
docker compose down -v
```

## What’s implemented so far (card-service focus)
- OAuth2 Resource Server with JWT: supports JWK set URI, issuer URI, or HS256 symmetric secret (Base64). Manual `JwtDecoder` bean plus startup validator to fail fast when security is enabled without config.
- Refresh tokens persisted in Postgres with device binding, last-used audit, reuse detection, and rotation.
- Swagger UI (`/swagger-ui.html`) with Bearer auth helper; OpenAPI metadata updated.
- Dev token mint endpoint (`/api/auth/token`) gated by `card.security.dev-token.enabled`.
- Resilience4j circuit breaker/time limiter defaults applied to outbound calls (balance verification, notifications).
- Actuator health/readiness with JWT health indicator; metrics/tracing wired for Micrometer + OTEL/Zipkin.
- H2 + Flyway-backed tests for refresh token lifecycle and circuit breaker wiring; all tests passing.

## Card Service quickstart (local)
```bash
cd services/card-service
mvn spring-boot:run
```

Key config (see `services/card-service/src/main/resources/application.yml`):
- `card.security.enabled=true`
- JWT secret (dev only): `spring.security.oauth2.resourceserver.jwt.secret-key=c2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24=` (Base64 for `secret-key-for-development-only-do-not-use-in-production`)
- Enable local mint endpoint if needed: `card.security.dev-token.enabled=true`
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- Actuator health/metrics: `http://localhost:8084/actuator/health`, `http://localhost:8084/actuator/metrics`
- Zipkin (if running): `http://localhost:9411/`

### Getting a JWT (HS256)
1) Use https://jwt.io or `node -e "..."` to sign HS256 with secret text: `secret-key-for-development-only-do-not-use-in-production` (no Base64 in the tool; the app decodes the Base64 property before verifying).
2) Minimal payload:
```json
{ "sub": "test-user", "scope": "cards.read cards.write", "iat": <now>, "exp": <now+1h> }
```
3) Call APIs with header: `Authorization: Bearer <token>`.

### Refresh tokens
- Issued via auth controller; stored in `refresh_tokens` table (Flyway migrations V8/V9).
- Device binding uses `X-Device-Id` and `User-Agent`; reuse of revoked/expired tokens revokes all subject tokens.
- Rotation endpoint returns a new refresh token and revokes the old one.
- Cookies: HTTP-only, `SameSite` configurable, precise `Max-Age` to avoid extra-second drift.

### Resilience, health, and observability
- Circuit breaker/time limiter instance `external-http` wraps outbound calls.
- Health readiness group includes DB, Kafka, and JWT decoder.
- Metrics via Micrometer; tracing via OTEL bridge + Zipkin reporter (start Zipkin locally to collect traces).

### Deployment, cache, and CI add-ons
- Cache metrics auto-bound to Micrometer; toggle via `card.cache.metrics.enabled` / `account.cache.metrics.enabled`. Local profile (`application-local.yml`) switches to simple cache and turns security off for quickstarts.
- Dockerfiles for card/account live under each service directory; Helm charts under `deploy/charts/*` with probes and security toggles.
- GitHub Actions workflow `.github/workflows/ci.yml` runs build/test, CycloneDX SBOM, OWASP dependency-check, Docker builds, and Helm lint.

## Account Service quickstart (local)
```bash
cd services/account-service
mvn spring-boot:run
```

Key config (see `services/account-service/src/main/resources/application.yml`):
- JWT resource server enabled; same bearer token flow as card-service with `SecurityStartupValidator`.
- Swagger UI: `http://localhost:8081/swagger-ui.html` (default port if unchanged).
- Actuator health/metrics: `http://localhost:8081/actuator/health`, `/actuator/metrics`.
- Kafka/Postgres properties inherit monorepo defaults; override via env or `application-local.yml`.

## Account Service API reference (essentials)
All endpoints require `Authorization: Bearer <JWT>` unless security toggle is off. Swagger UI lists the full set (`/swagger-ui.html`).

### Accounts
- `POST /api/accounts` create account. Request:
```json
{ "customerId": "<uuid>", "type": "CHECKING|SAVINGS|BROKERAGE", "currency": "USD", "initialDeposit": 100 }
```
- `GET /api/accounts/{id}` fetch; `GET /api/accounts?customerId=&page=&size=`.
- `PUT /api/accounts/{id}` update type/currency (partial).
- `PUT /api/accounts/{id}/status` with `{ "status": "ACTIVE|SUSPENDED|CLOSED" }`.
- `DELETE /api/accounts/{id}` hard delete.

### Account transactions
- `POST /api/accounts/{id}/transactions` apply credit/debit (idempotent by `referenceId`). Request:
```json
{ "referenceId": "<uuid>", "type": "CREDIT|DEBIT", "amount": 25.50, "description": "Deposit" }
```
- `GET /api/accounts/{id}/transactions?page=&size=` paged history.
- `GET /api/accounts/{id}/balance` returns current balance/currency.

### Bulk operations
- `POST /api/accounts/bulk` create up to 100 accounts:
```json
{ "customerId": "<uuid>", "accounts": [ { "type": "CHECKING", "currency": "USD", "initialDeposit": 50 } ] }
```
- `PUT /api/accounts/bulk/status` bulk status update:
```json
{ "accountIds": ["<uuid>"], "status": "SUSPENDED" }
```
- `POST /api/accounts/bulk/transactions` bulk transaction processing:
```json
{ "transactions": [ { "accountId": "<uuid>", "referenceId": "<uuid>", "type": "DEBIT", "amount": 10 } ] }
```

### Account goals (savings)
- `POST /api/accounts/{accountId}/goals` create goal:
```json
{ "name": "Emergency Fund", "targetAmount": 1000, "autoSweepEnabled": true, "autoSweepAmount": 50, "autoSweepCadence": "MONTHLY" }
```
- `GET /api/accounts/{accountId}/goals?page=&size=` list; `GET /.../{goalId}` fetch.
- `PUT /api/accounts/{accountId}/goals/{goalId}` update goal details/status/auto-sweep.
- `POST /api/accounts/{accountId}/goals/{goalId}/contributions` contribute (debits account):
```json
{ "amount": 25, "source": "MANUAL", "description": "Top-up", "referenceId": "<uuid>" }
```

### Account insights & audit
- Account insights controller provides simple derived metrics (see Swagger).
- `GET /api/admin/audit/events?...` lists Kafka/event audit logs; requires `accounts.admin`.

### Common responses
- `PageResponse<T>` used across list endpoints.
- Validation errors → 400; not-found → 404; optimistic lock/concurrency → 409 on transaction apply when conflicting.

## Account Service data model (Flyway)
- `accounts`: id, account_number (unique), customer_id, type, status, currency, balance, optimistic-lock `version`, soft-delete `deleted_at`, timestamps.
- `account_transactions`: per-account ledger entries with reference_id (idempotency), type (CREDIT/DEBIT), amount, resulting_balance, created_at; partition_key added for future partitioning.
- `account_transactions_archive`: same columns for archived history.
- `account_goals`: goal metadata, amounts, status, auto-sweep config, cadence, due dates, sweep tracking.
- `account_goal_contributions`: contributions to goals with reference_id for idempotency.
- `event_audit_logs`: Kafka/event audit trail with topic/direction/status/offsets/payload/error.

## Card Service API reference (essentials)
All endpoints require `Authorization: Bearer <JWT>` unless security toggle is off. Swagger UI lists the full set (`/swagger-ui.html`).

### Authentication helpers
- `GET /api/auth/me` → whoami; returns subject, issuer, scopes, authorities.
- `POST /api/auth/refresh-token` → issues opaque refresh token; headers `X-Device-Id`, `User-Agent` captured.
- `POST /api/auth/refresh` → validates + rotates opaque refresh token (body `{ "refreshToken": "<token>" }` or cookie `refresh_token`).
- `POST /api/auth/logout` → revokes refresh token and clears cookie.
- `POST /api/auth/token` → mint HS256 JWT for local dev when `card.security.dev-token.enabled=true`. Body (optional):
```json
{ "subject": "dev-user", "scope": "cards.read cards.write", "ttlSeconds": 3600 }
```

### Cards
- `POST /api/cards` create card. Request:
```json
{ "customerId": "<uuid>", "type": "DEBIT|CREDIT|PREPAID", "currency": "USD", "spendingLimit": 1000, "accountId": "<uuid>", "cardholderName": "Jane Doe" }
```
- `GET /api/cards/{id}` fetch; `GET /api/cards?customerId=&page=&size=`.
- Mutations: `PUT /{id}/activate`, `/block`, `/limit`, `/cancel`, `/transaction-limits`, `/pin`, `/pin/change`, `/pin/reset-attempts`, `/freeze`, `/unfreeze`, `/cvv/rotate`, `/atm-limits`, `/account-link`, `/account-link/unlink`, `POST /{id}/replace`, `POST /{id}/renew`, `PUT /{id}/contactless/enable|disable`.
- Responses use `CardResponse` (masked number, status, limits, pin/freeze/contactless flags, renewal/replacement info, account link).

### Transactions
- `POST /api/cards/{cardId}/transactions` create transaction. Request:
```json
{ "transactionType": "PURCHASE|ATM|CASH_WITHDRAWAL", "amount": 25.50, "currency": "USD", "merchantName": "Store", "merchantCategoryCode": "5411", "merchantCountry": "US" }
```
- `GET /api/cards/{cardId}/transactions?page=&size=` paged history; response `PageResponse<CardTransactionResponse>`.

### Transfers
- `POST /api/cards/{fromCardId}/transfers` card-to-card transfer. Request:
```json
{ "toCardId": "<uuid>", "amount": 50, "currency": "USD" }
```
- Response: transfer status, amounts, failure reason if any.

### Authorization (real-time check)
- `POST /api/cards/{cardId}/authorize` request:
```json
{ "amount": 75.00, "currency": "USD", "merchantName": "Cafe", "merchantCategoryCode": "5814", "merchantCountry": "US" }
```
- Response includes approval/decline, decline reason, evaluated limits/restrictions.

### Balance verification
- `POST /api/cards/{cardId}/balance/verify` request:
```json
{ "amount": 100.00 }
```
- Response: sufficient flag, available balance, currency; circuit breaker wraps outbound balance call.

### Restrictions
- Merchant: `POST /api/cards/{cardId}/restrictions/merchant` with `{ "merchantCategoryCode": "5999", "action": "BLOCK" }`; list via `GET .../merchant`; delete via `DELETE .../merchant/{mcc}`.
- Geographic: `POST /api/cards/{cardId}/restrictions/geographic` with `{ "countryCode": "US", "action": "BLOCK" }`; list via `GET .../geographic`; delete via `DELETE .../geographic/{countryCode}`.

### Fraud detection
- `POST /api/cards/{cardId}/fraud/check?amount=100&merchantCountry=US` → velocity/location analysis result (`FraudCheckResponse`).

### Notifications
- `POST /api/cards/{cardId}/notifications` request:
```json
{ "notificationType": "TRANSACTION_ALERT", "channel": "EMAIL|SMS|PUSH", "subject": "Txn alert", "message": "You spent $10" }
```
- `GET /api/cards/{cardId}/notifications?page=&size=` paged history.

### Analytics
- `GET /api/cards/{cardId}/analytics` summary; `PUT /api/cards/{cardId}/analytics/refresh` recompute aggregates.

### Audit admin
- `GET /api/admin/audit/events?status=&direction=&topic=&page=&size=` lists event audit logs; requires `cards.admin`.

### Common responses
- `PageResponse<T>`: `{ "content": [...], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }`.
- Errors use standard Spring validation / `GlobalExceptionHandler` (400 with message, 404 when not found).

## Card Service data model (Flyway)
- `cards`: id, customer_id, card_number/masked_number, status/type, currency, spending_limit, tx limits, pin/freeze/expiration/contactless, renewal/replacement refs, account_id, audit timestamps.
- `card_transactions`: history with type, amount/currency, merchant info, status/decline_reason.
- `card_transfers`: from_card_id, to_card_id, amount/currency, status, failure_reason.
- `card_merchant_restrictions`, `card_geographic_restrictions`: action ALLOW/BLOCK per MCC or country.
- `authorization_requests`: amount/currency, merchant details, status/decline_reason, checked_at.
- `fraud_events`: event_type, severity, description, transaction_id, fraud_score, resolved flags.
- `card_notifications`: type, channel, subject/message, status, sent_at/failure_reason.
- `card_analytics`: totals/averages, declined count, last txn date, top MCC/country.
- `velocity_tracking`: counts and amounts per card/time window for fraud checks.
- `refresh_tokens`: token, subject, scope, issued_at/expires_at, revoked, revocation_reason, device_id, user_agent, last_used_at.

## Next Steps
1. Define shared domain contracts (Avro/JSON schemas) and add them to each service.
2. Introduce common starter libraries (e.g., `commons-kafka`, `commons-security`).
3. Implement CI pipelines to build/test all services.
4. Add cache/data profile tuning, SAST/SBOM, and fraud/analytics streaming enhancements.

