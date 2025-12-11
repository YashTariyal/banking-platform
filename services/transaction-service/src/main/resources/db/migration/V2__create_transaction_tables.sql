-- Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_reference_id ON transactions(reference_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_customer_id ON transactions(customer_id);
CREATE INDEX idx_transactions_from_account_id ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account_id ON transactions(to_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

