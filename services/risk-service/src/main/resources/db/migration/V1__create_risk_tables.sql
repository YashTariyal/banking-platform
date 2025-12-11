-- Risk Assessments Table
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    risk_score INTEGER NOT NULL,
    customer_id UUID,
    account_id UUID,
    amount NUMERIC(19, 2),
    currency VARCHAR(3),
    risk_factors TEXT,
    description VARCHAR(500),
    assessed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_assessments_risk_type ON risk_assessments(risk_type);
CREATE INDEX idx_risk_assessments_risk_level ON risk_assessments(risk_level);
CREATE INDEX idx_risk_assessments_customer_id ON risk_assessments(customer_id);
CREATE INDEX idx_risk_assessments_account_id ON risk_assessments(account_id);
CREATE INDEX idx_risk_assessments_entity_id ON risk_assessments(entity_id);
CREATE INDEX idx_risk_assessments_assessed_at ON risk_assessments(assessed_at DESC);

-- Risk Alerts Table
CREATE TABLE risk_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_assessment_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    risk_score INTEGER NOT NULL,
    customer_id UUID,
    account_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_alerts_assessment FOREIGN KEY (risk_assessment_id) REFERENCES risk_assessments(id) ON DELETE CASCADE
);

CREATE INDEX idx_risk_alerts_status ON risk_alerts(status);
CREATE INDEX idx_risk_alerts_risk_level ON risk_alerts(risk_level);
CREATE INDEX idx_risk_alerts_customer_id ON risk_alerts(customer_id);
CREATE INDEX idx_risk_alerts_account_id ON risk_alerts(account_id);
CREATE INDEX idx_risk_alerts_assessment_id ON risk_alerts(risk_assessment_id);
CREATE INDEX idx_risk_alerts_created_at ON risk_alerts(created_at DESC);

