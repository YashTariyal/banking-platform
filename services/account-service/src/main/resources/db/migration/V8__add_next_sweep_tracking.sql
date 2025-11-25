ALTER TABLE account_goals
    ADD COLUMN IF NOT EXISTS next_sweep_at TIMESTAMPTZ;

UPDATE account_goals
SET next_sweep_at = COALESCE(
        next_sweep_at,
        CASE
            WHEN last_sweep_at IS NOT NULL THEN last_sweep_at
            ELSE created_at
        END
    );

ALTER TABLE account_goals
    ALTER COLUMN next_sweep_at SET DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_account_goals_next_sweep
    ON account_goals (next_sweep_at)
    WHERE auto_sweep_enabled = true;

