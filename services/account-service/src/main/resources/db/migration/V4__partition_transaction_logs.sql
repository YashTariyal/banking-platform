-- Create partitioned table for transaction logs (if not using existing table)
-- Note: PostgreSQL partitioning requires creating a new partitioned table and migrating data
-- This migration adds partitioning support for future use

-- Add partition key column (year-month) for easier partitioning
-- We'll use a generated column based on created_at
ALTER TABLE account_transactions
    ADD COLUMN IF NOT EXISTS partition_key DATE GENERATED ALWAYS AS (DATE_TRUNC('month', created_at)) STORED;

-- Create index on partition_key for partition pruning
CREATE INDEX IF NOT EXISTS idx_transactions_partition_key ON account_transactions (partition_key);

-- Note: Actual table partitioning in PostgreSQL requires:
-- 1. Creating a new partitioned table
-- 2. Migrating data
-- 3. Renaming tables
-- This is a complex operation that should be done during maintenance window
-- For now, we add the partition_key column to enable future partitioning

