-- Add index on account_number for faster lookups
CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts (account_number);

-- Add index on status for filtering
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts (status);

-- Add index on account_type for filtering
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts (account_type);

-- Add soft delete column
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Add index on deleted_at for efficient filtering of non-deleted accounts
CREATE INDEX IF NOT EXISTS idx_accounts_deleted_at ON accounts (deleted_at) WHERE deleted_at IS NULL;

-- Add indexes on transaction logs for better query performance
CREATE INDEX IF NOT EXISTS idx_transactions_account_id_created_at ON account_transactions (account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON account_transactions (created_at DESC);

-- Add index on reference_id for idempotency lookups (though it's part of PK, this helps with certain queries)
CREATE INDEX IF NOT EXISTS idx_transactions_reference_id ON account_transactions (reference_id);

