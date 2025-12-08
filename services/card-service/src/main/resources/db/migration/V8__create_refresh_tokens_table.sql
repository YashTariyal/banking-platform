CREATE TABLE IF NOT EXISTS refresh_tokens (
    token VARCHAR(36) PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    scope VARCHAR(1000),
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_subject ON refresh_tokens (subject);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

