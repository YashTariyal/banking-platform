# Banking Platform (Spring Boot + Kafka)

This monorepo contains seventeen domain-driven banking microservices built with Java 21, Spring Boot 3, Kafka, Hibernate, and PostgreSQL/MySQL drivers. Each service runs independently but shares conventions for messaging, persistence, and observability. The platform includes an API Gateway, service discovery, scheduled jobs, document management, notifications, and health aggregation.

## Tech Stack
- Java 21, Spring Boot 3.3
- Spring Web, Data JPA (Hibernate), Validation
- Spring for Apache Kafka
- PostgreSQL (primary) + MySQL connector for cross-RDBMS support
- Actuator for health/metrics
- Maven multi-module build

## Modules
| Service | Responsibility | Port |
| --- | --- | --- |
| **Infrastructure** | | |
| Eureka Server | Service discovery & registry | 8761 |
| API Gateway | Edge routing, request aggregation | 8091 |
| **Core Banking** | | |
| Identity | Auth, sessions, MFA, RBAC, API keys | 8082 |
| Customer | PII, contact data, preferences | 8081 |
| Account | Account catalog, lifecycle, limits | 8080 |
| Ledger | Double-entry postings, balances | 8085 |
| Transaction | Payment orchestration & sagas | 8090 |
| Payment | External rail integrations | 8087 |
| Card | Card issuance, auth handling | 8084 |
| Loan | Applications, schedules, repayments | 8086 |
| **Risk & Compliance** | | |
| KYC | Onboarding workflows, screening | 8084 |
| Risk | Fraud/risk scoring, alerts | 8088 |
| Compliance | Regulatory reports, AML | 8083 |
| **Support Services** | | |
| Support | Back-office cases, manual overrides | 8092 |
| Scheduler | Scheduled jobs (interest, statements, EOD) | 8093 |
| Document | Document management, PDF generation | 8094 |
| **Observability & Notifications** | | |
| Health Aggregator | Service health monitoring, business metrics | 8095 |
| Notification | Email, SMS, push notifications, alerts | 8096 |

## Platform Overview (cross-cutting)
- **Service Discovery**: Eureka Server running on port 8761 provides service registry and discovery. All services auto-register on startup. Access dashboard at http://localhost:8761
- **Security/JWT**: All services are OAuth2 Resource Servers (Bearer JWT). Toggle per service via `<service>.security.enabled=false` for local/test. Dev HS256 secrets live in each `application.yml`; tests disable security and use H2.
- **AOP & Logging**: Request/response logging + PII masking via `RequestLoggingFilter` and `PiiMaskingFilter` with MDC correlationId. Event monitoring aspects audit Kafka publish/consume paths to `event_audit_logs` tables (where present).
- **Observability**: Micrometer metrics (HTTP/Kafka/custom counters) and tracing hooks (OTel/Zipkin-ready). Actuator health/metrics/prometheus exposed; sampling probability defaults to 1.0 for local/test.
- **Data & Migrations**: Flyway migrations per service under `services/<service>/src/main/resources/db/migration`. Production uses Postgres; tests use in-memory H2 with Flyway disabled via `spring.flyway.enabled=false`.
- **Kafka**: Standardized topics per service (see Kafka Topics section). Correlation IDs flow through logs and metrics.
- **Testing**: MockMvc/WebMvcTest slices disable filters; `SimpleMeterRegistry` supplied where needed; H2 profiles and security disabled in tests.

## Service Docs & API Links
Use Swagger UI for full payloads and the docs for deep dives. Access all services through the API Gateway at `http://localhost:8091`.

| Service | Swagger (local) | Gateway Path | Docs |
| --- | --- | --- | --- |
| Account | http://localhost:8080/swagger-ui.html | `/accounts/**` | `docs/account-service.md` |
| Customer | http://localhost:8081/swagger-ui.html | `/customers/**` | `docs/customer-service.md` |
| Identity | http://localhost:8082/swagger-ui.html | `/identity/**` | `docs/identity-service.md` |
| Compliance | http://localhost:8083/swagger-ui.html | `/compliance/**` | `docs/compliance-service.md` |
| KYC | http://localhost:8084/swagger-ui.html | `/kyc/**` | `docs/kyc-service.md` |
| Card | http://localhost:8084/swagger-ui.html | `/cards/**` | `docs/card-service.md` |
| Ledger | http://localhost:8085/swagger-ui.html | `/ledger/**` | `docs/ledger-service.md` |
| Loan | http://localhost:8086/swagger-ui.html | `/loans/**` | `docs/loan-service.md` |
| Payment | http://localhost:8087/swagger-ui.html | `/payments/**` | `docs/payment-service.md` |
| Risk | http://localhost:8088/swagger-ui.html | `/risk/**` | `docs/risk-service.md` |
| Transaction | http://localhost:8090/swagger-ui.html | `/transactions/**` | `docs/transaction-service.md` |
| Support | http://localhost:8092/swagger-ui.html | `/support/**` | `docs/support-service.md` |
| Scheduler | http://localhost:8093/swagger-ui.html | `/scheduler/**` | - |
| Document | http://localhost:8094/swagger-ui.html | `/documents/**` | - |
| Health Aggregator | http://localhost:8095/swagger-ui.html | `/health-status/**` | - |
| Notification | http://localhost:8096/swagger-ui.html | `/notifications/**` | - |

Schema quick reference: Flyway SQL lives under each service’s `src/main/resources/db/migration`.

## Implemented Services

The following services are fully implemented with domain entities, repositories, services, REST APIs, Kafka integration, comprehensive tests, and documentation:

- ✅ **Account Service** - Account catalog, lifecycle, transactions, goals, and bulk operations
- ✅ **Compliance Service** - AML monitoring, suspicious activity detection, and regulatory reporting
- ✅ **Customer Service** - Customer PII, contact information, and preferences management
- ✅ **Identity Service** - User authentication, session management, MFA, RBAC, API keys, password reset, email verification
- ✅ **KYC Service** - Onboarding workflows, document verification, and screening
- ✅ **Ledger Service** - Double-entry journals, ledger accounts, reversals, and balances
- ✅ **Loan Service** - Loan applications, approvals, schedules, and payments
- ✅ **Payment Service** - External payment rail integrations (ACH, Wire, SWIFT, etc.)
- ✅ **Risk Service** - Risk assessment, scoring, and alerting for transactions, accounts, customers, and payments
- ✅ **Support Service** - Back-office case management and manual override capabilities
- ✅ **Transaction Service** - Transaction orchestration and lifecycle management for banking operations
- ✅ **API Gateway** - Edge routing with path-based routing to all microservices
- ✅ **Scheduler Service** - Scheduled jobs for interest calculation, statement generation, EOD reconciliation
- ✅ **Document Service** - Document upload/download, KYC document management, PDF statement generation
- ✅ **Health Aggregator** - Aggregates health status from all services, custom business metrics
- ✅ **Notification Service** - Multi-channel notifications (email, SMS, push), event-driven alerts

Each implemented service includes:
- Domain entities with JPA annotations
- Flyway database migrations
- Repository layer with custom queries
- Service layer with business logic
- REST controllers with Swagger/OpenAPI documentation
- Kafka event publishers and consumers
- Comprehensive test suites
- Service-specific documentation

Each module currently exposes a minimal Spring Boot bootstrap class, Kafka + datasource configs, and a dedicated Maven `pom.xml`. Extend each service with controllers, entities, repositories, and messaging adapters as requirements evolve.

Service-specific runbooks and API docs live under `docs/`:
- [Account Service](docs/account-service.md)
- [Compliance Service](docs/compliance-service.md)
- [Customer Service](docs/customer-service.md)
- [Identity Service](docs/identity-service.md)
- [Ledger Service](docs/ledger-service.md)
- [Loan Service](docs/loan-service.md)
- [KYC Service](docs/kyc-service.md)
- [Payment Service](docs/payment-service.md)
- [Risk Service](docs/risk-service.md)
- [Support Service](docs/support-service.md)
- [Transaction Service](docs/transaction-service.md)

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

## Security configuration (JWT + mTLS)

All business-domain services (account, customer, kyc, loan, risk, compliance, support, card) are designed to run as **OAuth2 Resource Servers** behind the Identity service. Each service exposes a `<service>.security.enabled` property to toggle enforcement, plus a startup validator that fails fast when security is misconfigured.

### JWT configuration (per service)

When `<service>.security.enabled=true`:

- **You must configure at least one of**:
  - `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` (preferred in prod)
  - `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  - `spring.security.oauth2.resourceserver.jwt.secret-key` (symmetric HS256, dev only)
- If none of these are set, the corresponding `SecurityStartupValidator` will throw on startup with a clear message (for example, `card.security.enabled=true but no JWT configuration found.`).

For Identity service (token issuer):

- Configure a strong 256‑bit secret:
  - `identity.jwt.secret-key=<random-32+ char secret>`
- Set token characteristics:
  - `identity.jwt.access-token-validity-seconds` (default: 3600)
  - `identity.jwt.refresh-token-validity-seconds` (default: 86400)
  - `identity.jwt.issuer=identity-service` (or your external issuer name)

### Local vs non-local profiles

- **Local/dev**:
  - It is acceptable to use `spring.security.oauth2.resourceserver.jwt.secret-key` with a known value and/or to disable security with `<service>.security.enabled=false` for fast iteration.
  - Tests set `<service>.security.enabled=false` explicitly where needed to keep suites fast and deterministic.
- **Non-local (staging/prod)**:
  - Prefer `jwk-set-uri` or `issuer-uri` pointing at your centralized IdP or Identity service.
  - Do **not** use the default secrets defined in `application.yml`; override via environment variables or a secrets manager.

### mTLS between services

For environments requiring mutual TLS on east‑west traffic:

- **Server-side TLS** (per service):
  - `server.ssl.enabled=true`
  - `server.ssl.key-store=classpath:tls/service-keystore.p12`
  - `server.ssl.key-store-password=<changeMe>`
  - `server.ssl.key-store-type=PKCS12`
  - `server.ssl.key-alias=<service-alias>`
- **Client trust configuration** (for HTTP clients such as `RestTemplate` / WebClient):
  - Configure a truststore with your internal CA:
    - `javax.net.ssl.trustStore=/etc/ssl/truststore.p12`
    - `javax.net.ssl.trustStorePassword=<changeMe>`
  - Or configure an HTTP client bean that loads a custom `SSLContext` wired to Spring’s `RestTemplate` / WebClient.
- **Certificate rotation**:
  - Prefer short-lived leaf certificates signed by an internal CA and automate renewal via your platform (Kubernetes cert-manager, SPIRE, etc.).

In Kubernetes, TLS keys and truststores should be mounted as Secrets and referenced from `application.yml` via environment variable placeholders (for example, `server.ssl.key-store=${SSL_KEYSTORE_PATH}`).

## Observability baseline (logs, metrics, traces)

The platform standardizes on **structured logging**, **Micrometer metrics**, and **OpenTelemetry-compatible tracing**:

- **Logging**:
  - JSON/structured logs via `logstash-logback-encoder` where enabled.
  - Correlation IDs propagated through MDC (see `RequestLoggingFilter` and `PiiMaskingFilter` in services such as account, customer, compliance, risk).
  - Console pattern includes `correlationId` to link API calls, Kafka events, and DB interactions.
- **Metrics**:
  - HTTP metrics via `http.server.requests`.
  - Domain metrics (for example: `accounts.transactions.amount`, `accounts.balance`) configured in `application.yml` with histogram and percentile settings.
  - Cache and Redis metrics are enabled where caching is used (for example: `account.cache.metrics.enabled=true`, `card.cache.metrics.enabled=true`).
- **Tracing**:
  - Micrometer Tracing with Zipkin reporter.
  - Sampling probability is set to `1.0` by default in local/test; adjust downwards in production to control cardinality and cost.

To expose operational endpoints consistently:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

Services such as `card-service`, `account-service`, and `identity-service` already follow this pattern; when adding a new service, copy these `management` and logging sections and adjust only the application name and any domain-specific metrics.

## Implemented Services Overview

### Account Service (Port 8081)
**Status**: ✅ Fully Implemented

**Features**:
- Account creation and lifecycle management (CHECKING, SAVINGS, BROKERAGE)
- Account transactions (credit/debit) with idempotency
- Account goals (savings goals with auto-sweep)
- Bulk operations (create, status update, transactions)
- Account insights and audit logging
- Kafka event publishing for account lifecycle events

**Quickstart**:
```bash
cd services/account-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/accounts` - Create account
- `GET /api/accounts/{id}` - Get account
- `POST /api/accounts/{id}/transactions` - Apply transaction
- `GET /api/accounts/{id}/balance` - Get balance
- `POST /api/accounts/{accountId}/goals` - Create savings goal

**Swagger UI**: `http://localhost:8081/swagger-ui.html`

**Database**: `account_service` (PostgreSQL)

**Tests**: Comprehensive unit and integration tests included

---

### Ledger Service (Port 8085)
**Status**: ✅ Fully Implemented

**Features**:
- Double-entry journals with balanced debits/credits
- Ledger accounts with types (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE)
- Automatic balance updates per account based on entry type and account type
- Journal reversal workflow that creates offsetting entries and marks originals as REVERSED
- Paging APIs for accounts and account entries

**Quickstart**:
```bash
cd services/ledger-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/ledger/accounts` - Create ledger account
- `GET /api/ledger/accounts/{id}` - Get ledger account
- `POST /api/ledger/journals` - Post balanced journal
- `POST /api/ledger/journals/{id}/reverse` - Reverse journal
- `GET /api/ledger/accounts/{accountId}/entries` - List account entries

**Swagger UI**: `http://localhost:8085/swagger-ui.html`

**Database**: `ledger_service` (PostgreSQL)

**Tests**: Covers journal posting, reversals, validations, and controllers

---

### Loan Service (Port 8086)
**Status**: ✅ Fully Implemented

**Features**:
- Loan applications with approval/rejection workflow
- Monthly amortization schedule generation on approval
- Payment schedule tracking with principal/interest breakdown
- Apply payments (interest first) and mark installments paid/late
- Retrieve schedules and payments via REST

**Quickstart**:
```bash
cd services/loan-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/loans` - Create loan application
- `POST /api/loans/{id}/approve` - Approve & generate schedule
- `POST /api/loans/{id}/reject` - Reject loan
- `GET /api/loans/{id}` - Get loan
- `GET /api/loans/{id}/schedule` - List schedule
- `POST /api/loans/{id}/payments` - Apply payment
- `GET /api/loans/{id}/payments` - List payments

**Swagger UI**: `http://localhost:8086/swagger-ui.html`

**Database**: `loan_service` (PostgreSQL)

**Tests**: Covers approvals/rejections, payment allocation, and controllers

---

### Compliance Service (Port 8083)
**Status**: ✅ Fully Implemented

**Features**:
- **AML Transaction Monitoring**: Automatic analysis of transactions from Kafka
  - Large cash transaction detection (≥ $10,000)
  - Structuring detection ($9,000-$9,999)
  - Round number pattern detection
  - Rapid movement detection
- **Risk Scoring**: Calculates risk scores (0-100) based on multiple factors
- **Suspicious Activity Management**: Automatic creation of suspicious activity records
- **Regulatory Reporting**: Generation and submission of SAR, CTR, LCTR reports
- **Kafka Integration**: Consumes transaction, payment, and card events

**Quickstart**:
```bash
cd services/compliance-service
mvn spring-boot:run
```

**Key Endpoints**:
- `GET /api/compliance/records` - List compliance records
- `GET /api/compliance/suspicious-activities` - List suspicious activities
- `POST /api/compliance/reports/generate` - Generate regulatory report
- `POST /api/compliance/reports/{id}/submit` - Submit report

**Swagger UI**: `http://localhost:8083/swagger-ui.html`

**Database**: `compliance_service` (PostgreSQL)

**Kafka Topics Consumed**: `transaction-events`, `payment-events`, `card-events`

**Kafka Topics Published**: `compliance-events`

**Tests**: Comprehensive test suite with 20+ tests

---

### Customer Service (Port 8081)
**Status**: ✅ Fully Implemented

**Features**:
- **Customer Management**: Create, update, delete customers with PII
- **Customer Number Generation**: Auto-generates unique customer numbers (CUST + 12 digits)
- **Contact Information**: Multiple contact methods (email, phone, addresses) with types
- **Customer Preferences**: Language, timezone, currency, notification settings
- **KYC Status Management**: Track and update KYC verification status
- **Kafka Integration**: Publishes customer lifecycle events

**Quickstart**:
```bash
cd services/customer-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer
- `PUT /api/customers/{id}` - Update customer
- `GET /api/customers/{customerId}/contact-info` - Get contact information
- `PUT /api/customers/{customerId}/preferences` - Update preferences

**Swagger UI**: `http://localhost:8081/swagger-ui.html`

**Database**: `customer_service` (PostgreSQL)

**Kafka Topics Published**: `customer-events` (CUSTOMER_CREATED, CUSTOMER_UPDATED, CUSTOMER_DELETED)

**Tests**: 17 tests covering service and controller layers

---

### Identity Service (Port 8082)
**Status**: ✅ Fully Implemented

**Features**:
- **User Authentication**: Registration, login/logout with JWT tokens
- **Session Management**: Track active sessions with device info, token refresh
- **Refresh Token Rotation**: One-time use refresh tokens with reuse detection (revokes all sessions on reuse)
- **Multi-Factor Authentication**: TOTP and SMS support with backup codes
- **Password Reset Flow**: Secure token-based password reset via email link
- **Email Verification**: Automatic verification email on registration
- **Role-Based Access Control (RBAC)**: Roles (CUSTOMER, EMPLOYEE, ADMIN) with granular permissions
- **API Key Authentication**: Machine-to-machine authentication with scopes and rate limits
- **Account Security**: Account locking after failed attempts, password hashing (BCrypt)
- **JWT Token Management**: Access tokens (1 hour) and refresh tokens (24 hours)
- **Kafka Integration**: Publishes user lifecycle events

**Quickstart**:
```bash
cd services/identity-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/auth/register` - Register new user (sends verification email)
- `POST /api/auth/login` - Login and get tokens
- `POST /api/auth/refresh` - Refresh tokens (with rotation)
- `POST /api/auth/logout` - Logout session
- `POST /api/auth/password/forgot` - Request password reset
- `POST /api/auth/password/reset` - Reset password with token
- `GET /api/auth/email/verify` - Verify email address
- `POST /api/auth/email/resend` - Resend verification email
- `GET /api/mfa/{userId}` - Get MFA settings
- `POST /api/mfa/{userId}/totp` - Enable TOTP MFA
- `POST /api/mfa/{userId}/sms` - Enable SMS MFA
- `GET /api/roles` - List all roles
- `POST /api/roles/assign` - Assign role to user
- `GET /api/roles/users/{userId}/permissions` - Get user permissions
- `POST /api/apikeys` - Create API key
- `DELETE /api/apikeys/{id}` - Revoke API key
- `POST /api/apikeys/validate` - Validate API key

**Swagger UI**: `http://localhost:8082/swagger-ui.html`

**Database**: `identity_service` (PostgreSQL)

**Kafka Topics Published**: `identity-events` (USER_REGISTERED, USER_LOGGED_IN, USER_LOGGED_OUT, USER_LOCKED, PASSWORD_RESET_REQUESTED, PASSWORD_CHANGED, EMAIL_VERIFICATION_REQUESTED, EMAIL_VERIFIED)

**Security**: BCrypt password hashing (strength 12), JWT with HMAC-SHA256, refresh token rotation

**Tests**: 30+ tests covering authentication, session, MFA, RBAC, and API key services

---

### KYC Service (Port 8084)
**Status**: ✅ Fully Implemented

**Features**:
- **Automatic Case Creation**: Creates onboarding KYC cases when customers register (via Kafka)
- **KYC Case Management**: Track cases through workflow (PENDING → DOCUMENT_REVIEW → SCREENING → UNDER_REVIEW → APPROVED/REJECTED)
- **Document Verification**: Upload and verify documents (passport, driver's license, proof of address, etc.)
- **Screening**: Sanctions, PEP, adverse media, and watchlist screening
- **Approval Workflow**: Approve/reject cases with notes and reasons
- **Risk Level Assignment**: LOW, MEDIUM, HIGH, CRITICAL
- **Kafka Integration**: Consumes customer events, publishes KYC lifecycle events

**Quickstart**:
```bash
cd services/kyc-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/kyc/cases` - Create KYC case
- `GET /api/kyc/cases/{id}` - Get KYC case
- `POST /api/kyc/cases/{id}/approve` - Approve case
- `POST /api/kyc/documents` - Upload document
- `PUT /api/kyc/documents/{id}/verify` - Verify document
- `POST /api/kyc/screening` - Perform screening

**Swagger UI**: `http://localhost:8084/swagger-ui.html`

**Database**: `kyc_service` (PostgreSQL)

**Kafka Topics Consumed**: `customer-events` (CUSTOMER_CREATED)

**Kafka Topics Published**: `kyc-events` (KYC_CASE_CREATED, KYC_CASE_UPDATED, KYC_CASE_APPROVED, KYC_CASE_REJECTED)

**Tests**: 11 tests covering KYC case, document, and screening services

---

### Payment Service (Port 8087)
**Status**: ✅ Fully Implemented

**Features**:
- **Payment Rails**: Support for ACH, Wire, SWIFT, FedWire, RTP, and Card Network
- **Payment Lifecycle**: PENDING → PROCESSING → COMPLETED/FAILED workflow
- **Payment Directions**: INBOUND and OUTBOUND payments
- **External Account Support**: Payments to external accounts with routing information
- **Idempotency**: Payments identified by unique reference ID
- **Payment Cancellation**: Cancel pending payments
- **Kafka Integration**: Publishes payment lifecycle events

**Quickstart**:
```bash
cd services/payment-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/payments` - Create payment
- `GET /api/payments/{id}` - Get payment
- `GET /api/payments/reference/{referenceId}` - Get payment by reference ID
- `GET /api/payments/account/{accountId}` - Get payments by account
- `PUT /api/payments/{id}/process` - Process payment
- `PUT /api/payments/{id}/complete` - Complete payment
- `PUT /api/payments/{id}/fail` - Fail payment
- `PUT /api/payments/{id}/cancel` - Cancel payment

**Swagger UI**: `http://localhost:8087/swagger-ui.html`

**Database**: `payment_service` (PostgreSQL)

**Kafka Topics Published**: `payment-events` (PAYMENT_INITIATED, PAYMENT_PROCESSING, PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_CANCELLED)

**Tests**: 6 tests covering payment service and controller

**Documentation**: [Payment Service](docs/payment-service.md)

---

### Risk Service (Port 8088)
**Status**: ✅ Fully Implemented

**Features**:
- **Risk Assessment**: Evaluate risk for transactions, accounts, customers, payments, cards, and loans
- **Risk Scoring**: Calculate risk scores based on multiple factors (amount, velocity, location, time patterns)
- **Risk Alerts**: Automatically generate alerts for medium-to-high risk assessments
- **Alert Management**: Review, resolve, and manage risk alerts
- **Risk Levels**: LOW, MEDIUM, HIGH, CRITICAL based on risk score thresholds
- **Kafka Integration**: Publishes risk assessment and alert events

**Quickstart**:
```bash
cd services/risk-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/risk/assessments` - Assess risk for an entity
- `GET /api/risk/assessments/{id}` - Get risk assessment
- `GET /api/risk/assessments` - List risk assessments (filterable by type, level, customer, account)
- `GET /api/risk/assessments/entity/{riskType}/{entityId}` - Get assessments by entity
- `GET /api/risk/alerts/{id}` - Get risk alert
- `GET /api/risk/alerts` - List risk alerts (filterable by status, level, customer, account)
- `PUT /api/risk/alerts/{id}/status` - Update alert status

**Swagger UI**: `http://localhost:8088/swagger-ui.html`

**Database**: `risk_service` (PostgreSQL)

**Kafka Topics Published**: `risk-events` (RISK_ASSESSMENT_CREATED, RISK_ALERT_CREATED, RISK_ALERT_UPDATED)

**Tests**: 25 tests (7 scoring, 6 assessment, 6 alert, 6 controller)

**Documentation**: [Risk Service](docs/risk-service.md)

---

### Support Service (Port 8092)
**Status**: ✅ Fully Implemented

**Features**:
- **Support Case Management**: Create, assign, track, and resolve customer support cases
- **Case Workflow**: Manage cases through lifecycle (OPEN → ASSIGNED → IN_PROGRESS → RESOLVED/CLOSED)
- **Manual Overrides**: Request, approve, and manage manual overrides for account limits, transaction approvals, fee waivers, etc.
- **Override Workflow**: PENDING → APPROVED/REJECTED workflow with expiration support
- **Case Types**: Support for account inquiries, transaction disputes, fraud reports, fee disputes, etc.
- **Case Priorities**: LOW, MEDIUM, HIGH, URGENT
- **Kafka Integration**: Publishes case and override lifecycle events

**Quickstart**:
```bash
cd services/support-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/support/cases` - Create support case
- `GET /api/support/cases/{id}` - Get support case
- `GET /api/support/cases/number/{caseNumber}` - Get case by number
- `GET /api/support/cases` - List cases (filterable by status, priority, type, customer, assignedTo)
- `PUT /api/support/cases/{id}/assign` - Assign case
- `PUT /api/support/cases/{id}/status` - Update case status
- `PUT /api/support/cases/{id}/resolve` - Resolve case
- `POST /api/support/overrides` - Create manual override
- `GET /api/support/overrides/{id}` - Get override
- `GET /api/support/overrides` - List overrides (filterable by status, type, customer, account)
- `GET /api/support/overrides/active/{customerId}/{overrideType}` - Get active overrides
- `PUT /api/support/overrides/{id}/approve` - Approve override
- `PUT /api/support/overrides/{id}/reject` - Reject override
- `PUT /api/support/overrides/{id}/revoke` - Revoke override

**Swagger UI**: `http://localhost:8092/swagger-ui.html`

**Database**: `support_service` (PostgreSQL)

**Kafka Topics Published**: `support-events` (SUPPORT_CASE_CREATED, SUPPORT_CASE_UPDATED, SUPPORT_CASE_RESOLVED, MANUAL_OVERRIDE_CREATED, MANUAL_OVERRIDE_APPROVED, MANUAL_OVERRIDE_REJECTED, MANUAL_OVERRIDE_REVOKED)

**Tests**: 21 tests (2 case number generator, 6 case service, 7 override service, 6 controller)

**Documentation**: [Support Service](docs/support-service.md)

---

### Transaction Service (Port 8090)
**Status**: ✅ Fully Implemented

**Features**:
- **Transaction Orchestration**: Create and manage transactions with idempotency support
- **Transaction Lifecycle**: PENDING → PROCESSING → COMPLETED/FAILED workflow
- **Transaction Types**: Support for deposits, withdrawals, transfers, payments, fees, interest, refunds, reversals, and adjustments
- **Transaction Reversal**: Reverse completed transactions
- **Transaction Cancellation**: Cancel pending transactions
- **Idempotency**: Prevent duplicate transactions via reference ID
- **Kafka Integration**: Publishes transaction lifecycle events for other services

**Quickstart**:
```bash
cd services/transaction-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/transactions` - Create transaction
- `GET /api/transactions/{id}` - Get transaction
- `GET /api/transactions/reference/{referenceId}` - Get transaction by reference ID
- `GET /api/transactions` - List transactions (filterable by status, type, customer, account)
- `PUT /api/transactions/{id}/process` - Process transaction
- `PUT /api/transactions/{id}/complete` - Complete transaction
- `PUT /api/transactions/{id}/fail` - Fail transaction
- `PUT /api/transactions/{id}/cancel` - Cancel transaction
- `PUT /api/transactions/{id}/reverse` - Reverse transaction

**Swagger UI**: `http://localhost:8090/swagger-ui.html`

**Database**: `transaction_service` (PostgreSQL)

**Kafka Topics Published**: `transaction-events` (TRANSACTION_INITIATED, TRANSACTION_PROCESSING, TRANSACTION_COMPLETED, TRANSACTION_FAILED, TRANSACTION_CANCELLED, TRANSACTION_REVERSED)

**Tests**: 11 tests (7 service, 4 controller)

**Documentation**: [Transaction Service](docs/transaction-service.md)

---

### API Gateway (Port 8091)
**Status**: ✅ Fully Implemented

**Features**:
- **Path-Based Routing**: Routes requests to appropriate microservices
- **Request Aggregation**: Single entry point for all APIs
- **Load Balancing**: Eureka-aware service discovery
- **Observability**: Integrated with Micrometer tracing

**Quickstart**:
```bash
cd services/api-gateway
mvn spring-boot:run
```

**Gateway Routes**:
| Path | Backend Service |
|------|-----------------|
| `/identity/**` | Identity Service (8082) |
| `/customers/**` | Customer Service (8081) |
| `/accounts/**` | Account Service (8080) |
| `/transactions/**` | Transaction Service (8090) |
| `/payments/**` | Payment Service (8087) |
| `/loans/**` | Loan Service (8086) |
| `/cards/**` | Card Service (8084) |
| `/kyc/**` | KYC Service (8084) |
| `/compliance/**` | Compliance Service (8083) |
| `/risk/**` | Risk Service (8088) |
| `/ledger/**` | Ledger Service (8085) |
| `/support/**` | Support Service (8092) |
| `/scheduler/**` | Scheduler Service (8093) |
| `/documents/**` | Document Service (8094) |
| `/health-status/**` | Health Aggregator (8095) |
| `/notifications/**` | Notification Service (8096) |

---

### Scheduler Service (Port 8093)
**Status**: ✅ Fully Implemented

**Features**:
- **Interest Calculation Job**: Daily interest calculation (2 AM)
- **Statement Generation Job**: Monthly statement generation (1st of each month, 3 AM)
- **EOD Reconciliation Job**: Daily end-of-day reconciliation (11 PM)
- **Manual Triggers**: API endpoints to manually trigger jobs
- **Job History**: Track job executions with status, records processed, errors

**Quickstart**:
```bash
cd services/scheduler-service
mvn spring-boot:run
```

**Key Endpoints**:
- `GET /api/scheduler/jobs` - List all scheduled jobs
- `GET /api/scheduler/jobs/{id}` - Get job by ID
- `POST /api/scheduler/jobs/interest-calculation/trigger` - Manually trigger interest calculation
- `POST /api/scheduler/jobs/statement-generation/trigger` - Manually trigger statement generation
- `POST /api/scheduler/jobs/eod-reconciliation/trigger` - Manually trigger EOD reconciliation
- `POST /api/scheduler/jobs/{id}/cancel` - Cancel a pending/running job

**Swagger UI**: `http://localhost:8093/swagger-ui.html`

**Database**: `scheduler_service` (PostgreSQL)

**Kafka Topics Published**: `scheduler-events`

---

### Document Service (Port 8094)
**Status**: ✅ Fully Implemented

**Features**:
- **Document Upload/Download**: Secure document storage with checksums
- **Document Types**: KYC documents, statements, contracts, tax documents
- **Document Verification**: Workflow for document approval/rejection
- **PDF Statement Generation**: Generate account statements with iText
- **Document Categories**: KYC, STATEMENT, CONTRACT, TAX, CORRESPONDENCE

**Quickstart**:
```bash
cd services/document-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/documents` - Upload document
- `GET /api/documents/{id}` - Get document metadata
- `GET /api/documents/{id}/download` - Download document
- `GET /api/documents/customer/{customerId}` - Get customer documents
- `PUT /api/documents/{id}/verify` - Verify document
- `PUT /api/documents/{id}/reject` - Reject document
- `POST /api/statements/generate` - Generate account statement PDF
- `POST /api/statements/preview` - Preview statement PDF
- `GET /api/statements/{id}/download` - Download generated statement

**Swagger UI**: `http://localhost:8094/swagger-ui.html`

**Database**: `document_service` (PostgreSQL)

**Kafka Topics Published**: `document-events`

---

### Health Aggregator Service (Port 8095)
**Status**: ✅ Fully Implemented

**Features**:
- **Service Health Monitoring**: Aggregates health status from all microservices
- **Scheduled Health Checks**: Periodic health checks (configurable interval)
- **Business Metrics**: Track transactions/min, approval rates, login success rates
- **Prometheus Integration**: Exposes metrics for monitoring dashboards

**Quickstart**:
```bash
cd services/health-aggregator
mvn spring-boot:run
```

**Key Endpoints**:
- `GET /api/health` - Get aggregated health status (UP, DOWN, DEGRADED)
- `GET /api/health/services` - Get health status of all services
- `GET /api/health/services/{serviceName}` - Get health of specific service
- `GET /api/health/metrics` - Get business metrics summary
- `POST /api/health/metrics/transaction` - Record transaction metric
- `POST /api/health/metrics/login` - Record login metric
- `POST /api/health/metrics/loan` - Record loan application metric

**Swagger UI**: `http://localhost:8095/swagger-ui.html`

**Prometheus Metrics**:
- `service.health` - Health status per service (1=UP, 0=DOWN)
- `service.response.time` - Response time per service
- `banking.transactions.total` - Total transactions
- `banking.transactions.approved` - Approved transactions
- `banking.loans.applications.total` - Total loan applications
- `banking.auth.login.attempts` - Login attempts

---

### Notification Service (Port 8096)
**Status**: ✅ Fully Implemented

**Features**:
- **Multi-Channel Notifications**: Email, SMS, Push, In-App
- **Event-Driven Alerts**: Automatic notifications from Kafka events
- **Alert Types**: Low balance, suspicious activity, payment received, password reset
- **Notification Preferences**: Per-customer channel preferences
- **Template Support**: Thymeleaf templates for HTML emails
- **Retry Mechanism**: Automatic retry for failed notifications

**Event-Driven Alerts**:
- Low Balance Alert: When balance drops below $100
- Suspicious Activity: When transaction exceeds $10,000
- Payment Received: When payment is completed
- Password Reset: When password reset is requested
- Email Verification: When user registers

**Quickstart**:
```bash
cd services/notification-service
mvn spring-boot:run
```

**Key Endpoints**:
- `POST /api/notifications` - Send notification
- `GET /api/notifications/customer/{customerId}` - Get customer notifications
- `GET /api/notifications/customer/{customerId}/unread` - Get unread notifications
- `GET /api/notifications/customer/{customerId}/unread/count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark notification as read
- `PUT /api/notifications/customer/{customerId}/read-all` - Mark all as read
- `GET /api/notifications/preferences/{customerId}` - Get notification preferences
- `PUT /api/notifications/preferences/{customerId}` - Update preferences

**Swagger UI**: `http://localhost:8096/swagger-ui.html`

**Database**: `notification_service` (PostgreSQL)

**Kafka Topics Consumed**: `identity-events`, `transaction-events`, `account-events`

---

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

## Service Integration Flow

The implemented services integrate via Kafka events:

1. **Customer Registration Flow**:
   - Customer Service creates customer → publishes `CUSTOMER_CREATED` event
   - KYC Service consumes event → automatically creates onboarding KYC case
   - Identity Service can create user account linked to customer

2. **KYC Approval Flow**:
   - Documents uploaded and verified in KYC Service
   - Screening performed (sanctions, PEP)
   - KYC case approved → publishes `KYC_CASE_APPROVED` event
   - Customer Service updates customer KYC status to VERIFIED

3. **Transaction Monitoring Flow**:
   - Account/Transaction/Payment/Card services publish transaction events
   - Compliance Service consumes events → performs AML analysis
   - Creates compliance records and suspicious activities for high-risk transactions

4. **Authentication Flow**:
   - Identity Service handles user registration and authentication
   - Issues JWT access and refresh tokens
   - Other services validate JWT tokens for API access

## Service Ports Summary

| Service | Port | Status | Database |
|---------|------|--------|----------|
| **Infrastructure** | | | |
| Eureka Server | 8761 | ✅ Implemented | - |
| API Gateway | 8091 | ✅ Implemented | - |
| **Core Banking** | | | |
| Account Service | 8080 | ✅ Implemented | `account_service` |
| Customer Service | 8081 | ✅ Implemented | `customer_service` |
| Identity Service | 8082 | ✅ Implemented | `identity_service` |
| Compliance Service | 8083 | ✅ Implemented | `compliance_service` |
| Card Service | 8084 | ✅ Implemented | `card_service` |
| KYC Service | 8084 | ✅ Implemented | `kyc_service` |
| Ledger Service | 8085 | ✅ Implemented | `ledger_service` |
| Loan Service | 8086 | ✅ Implemented | `loan_service` |
| Payment Service | 8087 | ✅ Implemented | `payment_service` |
| Risk Service | 8088 | ✅ Implemented | `risk_service` |
| Transaction Service | 8090 | ✅ Implemented | `transaction_service` |
| Support Service | 8092 | ✅ Implemented | `support_service` |
| **Support Services** | | | |
| Scheduler Service | 8093 | ✅ Implemented | `scheduler_service` |
| Document Service | 8094 | ✅ Implemented | `document_service` |
| **Observability** | | | |
| Health Aggregator | 8095 | ✅ Implemented | - |
| Notification Service | 8096 | ✅ Implemented | `notification_service` |

## Kafka Topics Used

### Topics Published
- `customer-events` (Customer Service)
- `identity-events` (Identity Service)
- `kyc-events` (KYC Service)
- `compliance-events` (Compliance Service)
- `account-events` (Account Service)
- `ledger-events` (Ledger Service)
- `loan-events` (Loan Service)
- `payment-events` (Payment Service)
- `risk-events` (Risk Service)
- `support-events` (Support Service)
- `transaction-events` (Transaction Service)
- `card-events` (Card Service)
- `scheduler-events` (Scheduler Service)
- `document-events` (Document Service)

### Topics Consumed
- `customer-events` → KYC Service (creates onboarding cases)
- `transaction-events` → Compliance Service (AML monitoring), Notification Service (alerts)
- `payment-events` → Compliance Service (AML monitoring)
- `card-events` → Compliance Service (AML monitoring)
- `identity-events` → Notification Service (password reset, email verification)
- `account-events` → Notification Service (low balance alerts)

## Testing

All implemented services include comprehensive test suites:

- **Account Service**: Unit and integration tests for accounts, transactions, goals
- **Compliance Service**: 20+ tests for AML analysis, suspicious activities, reports
- **Customer Service**: 17 tests for customer CRUD, contact info, preferences
- **Identity Service**: 21 tests for authentication, sessions, MFA
- **KYC Service**: 11 tests for KYC cases, documents, screening

Run tests for a specific service:
```bash
cd services/<service-name>
mvn test
```

Run all tests:
```bash
mvn test
```

## Running All Services

Use the provided script to start all services at once:

```bash
./run-all-services.sh
```

This starts all 17 microservices in the background with logs in `logs/`. Press Ctrl+C to stop all services.

Or run individual services:
```bash
cd services/<service-name>
mvn spring-boot:run
```

## Next Steps
1. Define shared domain contracts (Avro/JSON schemas) and add them to each service.
2. Introduce common starter libraries (e.g., `commons-kafka`, `commons-security`).
3. Implement CI pipelines to build/test all services.
4. Add cache/data profile tuning, SAST/SBOM, and fraud/analytics streaming enhancements.
5. Add centralized logging (ELK or Loki stack)
6. Set up distributed tracing dashboard (Jaeger/Zipkin UI)
7. Implement rate limiting in API Gateway
8. Add Redis caching for session management

