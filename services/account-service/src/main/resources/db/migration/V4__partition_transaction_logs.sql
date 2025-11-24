-- Note: PostgreSQL partitioning requires creating a new partitioned table and migrating data
-- This migration adds partitioning support for future use

-- Helper function to compute month-start date; marked IMMUTABLE so it can be used in generated column
CREATE OR REPLACE FUNCTION account_month_key(ts timestamptz)
RETURNS date
LANGUAGE sql
IMMUTABLE
AS $func$
    SELECT make_date(
        extract(year FROM ts)::int,
        extract(month FROM ts)::int,
        1
    );
$func$;

-- Add partition key column (year-month) for easier partitioning
-- We'll use a generated column based on created_at
ALTER TABLE account_transactions
    ADD COLUMN IF NOT EXISTS partition_key DATE GENERATED ALWAYS AS (account_month_key(created_at)) STORED;

-- Create index on partition_key for partition pruning
CREATE INDEX IF NOT EXISTS idx_transactions_partition_key ON account_transactions (partition_key);

-- Note: Actual table partitioning in PostgreSQL requires:
-- 1. Creating a new partitioned table
-- 2. Migrating data
-- 3. Renaming tables
-- This is a complex operation that should be done during maintenance window
-- For now, we add the partition_key column to enable future partitioning

