# Ledger Service

The Ledger Service provides double-entry accounting for the banking platform. It maintains ledger accounts, posts balanced journals, tracks entries, and supports reversals with audit trails.

## Capabilities
- Ledger accounts with types (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE) and statuses (ACTIVE, SUSPENDED, CLOSED)
- Balanced journal posting (debits must equal credits)
- Automatic balance updates per account with correct debit/credit impact by account type
- Journal reversal workflow that creates offsetting entries and marks the original journal as reversed
- Paging APIs to retrieve ledger accounts and account entries
- Flyway migrations for ledger tables
- Kafka audit aspect already wired for event monitoring (shared audit tables)

## API (REST)

### Accounts
- `POST /api/ledger/accounts` — create ledger account  
  Request: `{ "name": "Cash", "type": "ASSET", "currency": "USD", "externalAccountId": "acct-123" }`
- `GET /api/ledger/accounts/{id}` — get account
- `GET /api/ledger/accounts?page=&size=` — list accounts (paged)
- `PUT /api/ledger/accounts/{id}/status` — update status (ACTIVE/SUSPENDED/CLOSED)

### Journals & Entries
- `POST /api/ledger/journals` — post balanced journal with entries  
  Each entry: `{ "ledgerAccountId": "<uuid>", "entryType": "DEBIT|CREDIT", "amount": 100.00, "currency": "USD" }`
- `POST /api/ledger/journals/{id}/reverse` — create reversal journal and mark original as REVERSED
- `GET /api/ledger/journals/{id}` — fetch journal with entries
- `GET /api/ledger/accounts/{accountId}/entries?page=&size=` — list entries for an account

## Data Model
- `ledger_accounts`: id, name, type, status, currency, external_account_id (unique), balance, version, timestamps
- `ledger_journals`: id, reference_id (unique), description, status, posted_at, reversal_of, timestamps
- `ledger_entries`: id, journal_id, ledger_account_id, entry_type, amount, currency, description, posted_at

## Running locally
```bash
cd services/ledger-service
mvn spring-boot:run
```
- Port: `8085`
- Swagger UI: `http://localhost:8085/swagger-ui.html`
- Database: `ledger_service` (PostgreSQL)

## Testing
```bash
cd services/ledger-service
mvn test
```
Tests cover:
- Ledger posting and balance adjustments
- Unbalanced journal validation
- Journal reversals
- REST controllers for accounts and journals

## Kafka audit (shared)
The module includes the event audit aspect (publish/consume) used across services. It records event metadata to `event_audit_logs` and exposes Micrometer counters for publish/consume success and failure.

