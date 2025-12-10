CREATE TABLE IF NOT EXISTS compliance_records (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID,
    transaction_id UUID,
    record_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4),
    currency CHAR(3),
    description TEXT,
    risk_score INTEGER,
    flags TEXT,
    source_event_type VARCHAR(128),
    source_topic VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compliance_records_customer_id ON compliance_records (customer_id);
CREATE INDEX IF NOT EXISTS idx_compliance_records_account_id ON compliance_records (account_id);
CREATE INDEX IF NOT EXISTS idx_compliance_records_transaction_id ON compliance_records (transaction_id);
CREATE INDEX IF NOT EXISTS idx_compliance_records_record_type ON compliance_records (record_type);
CREATE INDEX IF NOT EXISTS idx_compliance_records_status ON compliance_records (status);
CREATE INDEX IF NOT EXISTS idx_compliance_records_created_at ON compliance_records (created_at);

CREATE TABLE IF NOT EXISTS suspicious_activities (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID,
    transaction_id UUID,
    activity_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4),
    currency CHAR(3),
    description TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    compliance_record_id UUID,
    investigator_id UUID,
    investigation_notes TEXT,
    reported_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_suspicious_activities_customer_id ON suspicious_activities (customer_id);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_status ON suspicious_activities (status);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_severity ON suspicious_activities (severity);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_activity_type ON suspicious_activities (activity_type);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_created_at ON suspicious_activities (created_at);
CREATE INDEX IF NOT EXISTS idx_suspicious_activities_compliance_record_id ON suspicious_activities (compliance_record_id);

CREATE TABLE IF NOT EXISTS regulatory_reports (
    id UUID PRIMARY KEY,
    report_type VARCHAR(64) NOT NULL,
    report_period_start DATE NOT NULL,
    report_period_end DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    file_path VARCHAR(512),
    record_count INTEGER,
    total_amount NUMERIC(19, 4),
    submitted_at TIMESTAMPTZ,
    submitted_by UUID,
    regulatory_reference VARCHAR(128),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_regulatory_reports_report_type ON regulatory_reports (report_type);
CREATE INDEX IF NOT EXISTS idx_regulatory_reports_status ON regulatory_reports (status);
CREATE INDEX IF NOT EXISTS idx_regulatory_reports_period ON regulatory_reports (report_period_start, report_period_end);

