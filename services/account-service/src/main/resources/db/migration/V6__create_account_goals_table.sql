CREATE TABLE account_goals (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    target_amount NUMERIC(19,4) NOT NULL,
    current_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    due_date DATE,
    status VARCHAR(32) NOT NULL,
    auto_sweep_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auto_sweep_amount NUMERIC(19,4),
    auto_sweep_cadence VARCHAR(32),
    last_sweep_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_account_goals_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_account_goals_account_id ON account_goals(account_id);
CREATE INDEX idx_account_goals_status ON account_goals(status);
CREATE INDEX idx_account_goals_auto_sweep ON account_goals(auto_sweep_enabled, status);

CREATE TABLE account_goal_contributions (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    source VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    reference_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_goal_contributions_goal FOREIGN KEY (goal_id) REFERENCES account_goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_contributions_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT uq_goal_contribution_reference UNIQUE (account_id, reference_id)
);

CREATE INDEX idx_goal_contributions_goal_id ON account_goal_contributions(goal_id);

