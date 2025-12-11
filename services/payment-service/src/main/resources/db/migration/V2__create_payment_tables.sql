CREATE TABLE payments (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL UNIQUE,
    rail VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    from_account_id UUID NOT NULL,
    to_account_id UUID,
    to_external_account VARCHAR(255),
    to_external_routing VARCHAR(255),
    to_external_bank_name VARCHAR(255),
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    external_reference VARCHAR(255),
    initiated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_reference_id ON payments(reference_id);
CREATE INDEX idx_payments_from_account_id ON payments(from_account_id);
CREATE INDEX idx_payments_to_account_id ON payments(to_account_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_rail ON payments(rail);
CREATE INDEX idx_payments_created_at ON payments(created_at);

