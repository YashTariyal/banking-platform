CREATE TABLE IF NOT EXISTS kyc_cases (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32),
    case_type VARCHAR(32) NOT NULL,
    assigned_to UUID,
    review_notes TEXT,
    screening_completed BOOLEAN NOT NULL DEFAULT false,
    document_verification_completed BOOLEAN NOT NULL DEFAULT false,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_kyc_cases_customer_id ON kyc_cases (customer_id);
CREATE INDEX IF NOT EXISTS idx_kyc_cases_status ON kyc_cases (status);
CREATE INDEX IF NOT EXISTS idx_kyc_cases_case_type ON kyc_cases (case_type);
CREATE INDEX IF NOT EXISTS idx_kyc_cases_assigned_to ON kyc_cases (assigned_to);
CREATE INDEX IF NOT EXISTS idx_kyc_cases_due_date ON kyc_cases (due_date);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    kyc_case_id UUID NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    verification_status VARCHAR(32) NOT NULL,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    verification_notes TEXT,
    expiry_date TIMESTAMPTZ,
    document_number VARCHAR(100),
    issuing_country CHAR(2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_documents_kyc_case_id ON documents (kyc_case_id);
CREATE INDEX IF NOT EXISTS idx_documents_document_type ON documents (document_type);
CREATE INDEX IF NOT EXISTS idx_documents_verification_status ON documents (verification_status);

CREATE TABLE IF NOT EXISTS screening_results (
    id UUID PRIMARY KEY,
    kyc_case_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    screening_type VARCHAR(32) NOT NULL,
    result VARCHAR(32) NOT NULL,
    match_score INTEGER,
    matched_name VARCHAR(255),
    matched_list VARCHAR(100),
    match_details TEXT,
    screening_provider VARCHAR(100),
    screening_reference VARCHAR(255),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID,
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_screening_results_kyc_case_id ON screening_results (kyc_case_id);
CREATE INDEX IF NOT EXISTS idx_screening_results_customer_id ON screening_results (customer_id);
CREATE INDEX IF NOT EXISTS idx_screening_results_screening_type ON screening_results (screening_type);
CREATE INDEX IF NOT EXISTS idx_screening_results_result ON screening_results (result);

