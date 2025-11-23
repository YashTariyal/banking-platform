ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE accounts
SET version = 0
WHERE version IS NULL;

ALTER TABLE accounts
    ALTER COLUMN version DROP DEFAULT;

CREATE TABLE IF NOT EXISTS account_transactions (
    account_id UUID NOT NULL,
    reference_id UUID NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    resulting_balance NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id, reference_id)
);

