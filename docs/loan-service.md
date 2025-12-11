# Loan Service

The Loan Service manages loan applications, approvals, schedules, and payments with a simple amortized repayment model.

## Capabilities
- Create and track loan applications
- Approve or reject loans; approval generates a monthly amortization schedule
- Store payment schedules with principal/interest breakdown per installment
- Apply payments to the earliest due installment (interest first), mark installments paid/late
- Retrieve schedules and payments via REST
- Flyway-backed schema migrations
- Swagger/OpenAPI documentation

## API (REST)

### Loans
- `POST /api/loans` — create loan application  
  Request: `{ "customerId": "<uuid>", "principal": 1000.00, "annualInterestRate": 12.0, "termMonths": 12 }`
- `POST /api/loans/{id}/approve` — approve & generate schedule  
  Optional body: `{ "firstDueDate": "2024-02-01" }`
- `POST /api/loans/{id}/reject` — reject loan
- `GET /api/loans/{id}` — fetch loan
- `GET /api/loans?page=&size=&customerId=` — list loans (paged)

### Schedule & Payments
- `GET /api/loans/{id}/schedule` — list payment schedule items
- `POST /api/loans/{id}/payments` — apply payment  
  Request: `{ "amount": 50.00, "paymentType": "REGULAR", "paymentDate": "2024-01-15" }`
- `GET /api/loans/{id}/payments` — list payments

## Data Model
- `loan_applications`: id, customer_id, principal, annual_interest_rate, term_months, status, approved_at, rejected_at, disbursed_at, first_due_date, timestamps
- `payment_schedule_items`: id, loan_application_id, installment_number, due_date, principal_due, interest_due, principal_paid, interest_paid, status
- `loan_payments`: id, loan_application_id, payment_type, amount, principal_component, interest_component, payment_date, created_at

## Running locally
```bash
cd services/loan-service
mvn spring-boot:run
```
- Port: `8086`
- Swagger UI: `http://localhost:8086/swagger-ui.html`
- Database: `loan_service` (PostgreSQL)

## Testing
```bash
cd services/loan-service
mvn test
```
Tests cover:
- Loan approval/rejection logic and list filtering
- Payment allocation across schedule installments
- REST controllers for loans, schedules, and payments


