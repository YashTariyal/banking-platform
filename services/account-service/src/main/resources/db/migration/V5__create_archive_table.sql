-- Create archive table for old transactions
-- This table has the same structure as account_transactions but is used for archived data

CREATE TABLE IF NOT EXISTS account_transactions_archive (
    account_id UUID NOT NULL,
    reference_id UUID NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    resulting_balance NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_id, reference_id, archived_at)
);

-- Create indexes on archive table for efficient queries
CREATE INDEX IF NOT EXISTS idx_transactions_archive_account_id_created_at 
    ON account_transactions_archive (account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_archive_created_at 
    ON account_transactions_archive (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_archive_archived_at 
    ON account_transactions_archive (archived_at DESC);

-- Note: Actual partitioning would require:
-- 1. Creating partitioned table with PARTITION BY RANGE (created_at)
-- 2. Creating monthly partitions
-- 3. Migrating data
-- This is complex and should be done during maintenance window
-- The partition_key column in V4 migration can be used for future partitioning

