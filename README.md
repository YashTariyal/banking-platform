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

Service-specific runbooks and API docs (starting with the Account Service) live under `docs/`.

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

## Next Steps
1. Define shared domain contracts (Avro/JSON schemas) and add them to each service.
2. Introduce common starter libraries (e.g., `commons-kafka`, `commons-security`).
3. Add Docker Compose for Kafka, PostgreSQL, Schema Registry. ✅
4. Implement CI pipelines to build/test all services.

