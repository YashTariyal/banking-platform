CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts (customer_id);

