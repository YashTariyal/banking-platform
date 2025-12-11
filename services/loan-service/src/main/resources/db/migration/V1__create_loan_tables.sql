CREATE TABLE loan_applications
(
    id                    UUID PRIMARY KEY,
    customer_id           UUID            NOT NULL,
    principal             NUMERIC(19, 2)  NOT NULL,
    annual_interest_rate  NUMERIC(5, 3)   NOT NULL,
    term_months           INT             NOT NULL,
    status                VARCHAR(16)     NOT NULL,
    approved_at           TIMESTAMP,
    rejected_at           TIMESTAMP,
    disbursed_at          TIMESTAMP,
    first_due_date        DATE,
    created_at            TIMESTAMP       NOT NULL,
    updated_at            TIMESTAMP       NOT NULL
);

CREATE TABLE payment_schedule_items
(
    id                   UUID PRIMARY KEY,
    loan_application_id  UUID           NOT NULL REFERENCES loan_applications (id),
    installment_number   INT            NOT NULL,
    due_date             DATE           NOT NULL,
    principal_due        NUMERIC(19, 2) NOT NULL,
    interest_due         NUMERIC(19, 2) NOT NULL,
    principal_paid       NUMERIC(19, 2) NOT NULL DEFAULT 0,
    interest_paid        NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status               VARCHAR(8)     NOT NULL,
    CONSTRAINT uq_payment_schedule UNIQUE (loan_application_id, installment_number)
);

CREATE TABLE loan_payments
(
    id                   UUID PRIMARY KEY,
    loan_application_id  UUID           NOT NULL REFERENCES loan_applications (id),
    payment_type         VARCHAR(16)    NOT NULL,
    amount               NUMERIC(19, 2) NOT NULL,
    principal_component  NUMERIC(19, 2) NOT NULL,
    interest_component   NUMERIC(19, 2) NOT NULL,
    payment_date         DATE           NOT NULL,
    created_at           TIMESTAMP      NOT NULL
);

