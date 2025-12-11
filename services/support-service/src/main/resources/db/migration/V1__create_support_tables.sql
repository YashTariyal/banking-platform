-- Support Cases Table
CREATE TABLE support_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(20) UNIQUE NOT NULL,
    case_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    customer_id UUID NOT NULL,
    account_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to UUID,
    created_by UUID NOT NULL,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,
    due_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_support_cases_case_number ON support_cases(case_number);
CREATE INDEX idx_support_cases_status ON support_cases(status);
CREATE INDEX idx_support_cases_priority ON support_cases(priority);
CREATE INDEX idx_support_cases_case_type ON support_cases(case_type);
CREATE INDEX idx_support_cases_customer_id ON support_cases(customer_id);
CREATE INDEX idx_support_cases_assigned_to ON support_cases(assigned_to);
CREATE INDEX idx_support_cases_created_at ON support_cases(created_at DESC);

-- Manual Overrides Table
CREATE TABLE manual_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    override_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    customer_id UUID,
    account_id UUID,
    entity_id UUID,
    requested_by UUID NOT NULL,
    approved_by UUID,
    rejected_by UUID,
    reason TEXT NOT NULL,
    override_value TEXT,
    amount NUMERIC(19, 2),
    currency VARCHAR(3),
    expires_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manual_overrides_status ON manual_overrides(status);
CREATE INDEX idx_manual_overrides_override_type ON manual_overrides(override_type);
CREATE INDEX idx_manual_overrides_customer_id ON manual_overrides(customer_id);
CREATE INDEX idx_manual_overrides_account_id ON manual_overrides(account_id);
CREATE INDEX idx_manual_overrides_expires_at ON manual_overrides(expires_at);
CREATE INDEX idx_manual_overrides_created_at ON manual_overrides(created_at DESC);

