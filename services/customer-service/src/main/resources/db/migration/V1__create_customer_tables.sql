CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    customer_number VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    national_id VARCHAR(50),
    national_id_type VARCHAR(32),
    customer_type VARCHAR(32) NOT NULL,
    kyc_status VARCHAR(32),
    kyc_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_customers_customer_number ON customers (customer_number);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers (status);
CREATE INDEX IF NOT EXISTS idx_customers_customer_type ON customers (customer_type);
CREATE INDEX IF NOT EXISTS idx_customers_national_id ON customers (national_id, national_id_type);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers (created_at);

CREATE TABLE IF NOT EXISTS contact_info (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    contact_type VARCHAR(32) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country CHAR(2),
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_verified BOOLEAN,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_contact_info_customer_id ON contact_info (customer_id);
CREATE INDEX IF NOT EXISTS idx_contact_info_email ON contact_info (email);
CREATE INDEX IF NOT EXISTS idx_contact_info_phone ON contact_info (phone);
CREATE INDEX IF NOT EXISTS idx_contact_info_is_primary ON contact_info (customer_id, is_primary);

CREATE TABLE IF NOT EXISTS customer_preferences (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,
    language VARCHAR(10),
    timezone VARCHAR(50),
    currency CHAR(3),
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_notifications_enabled BOOLEAN NOT NULL DEFAULT false,
    push_notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    marketing_emails_enabled BOOLEAN NOT NULL DEFAULT false,
    paper_statements_enabled BOOLEAN NOT NULL DEFAULT false,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT false,
    biometric_enabled BOOLEAN,
    preferred_contact_method VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_preferences_customer_id ON customer_preferences (customer_id);

