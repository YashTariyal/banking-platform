-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    customer_id UUID,
    account_id UUID,
    document_type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500),
    metadata TEXT,
    expires_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_documents_customer_id ON documents (customer_id);
CREATE INDEX IF NOT EXISTS idx_documents_account_id ON documents (account_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents (document_type);
CREATE INDEX IF NOT EXISTS idx_documents_category ON documents (category);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);

-- Document access log for audit
CREATE TABLE IF NOT EXISTS document_access_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id),
    user_id UUID,
    access_type VARCHAR(20) NOT NULL, -- VIEW, DOWNLOAD, UPDATE, DELETE
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_doc_access_document_id ON document_access_log (document_id);
CREATE INDEX IF NOT EXISTS idx_doc_access_user_id ON document_access_log (user_id);
CREATE INDEX IF NOT EXISTS idx_doc_access_accessed_at ON document_access_log (accessed_at);
