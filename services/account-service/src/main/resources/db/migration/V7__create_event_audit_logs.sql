CREATE TABLE event_audit_logs (
    id UUID PRIMARY KEY,
    direction VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_type VARCHAR(128),
    event_key VARCHAR(255),
    payload TEXT,
    record_partition INTEGER,
    record_offset BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_audit_logs_topic_direction ON event_audit_logs(topic, direction);
CREATE INDEX idx_event_audit_logs_status ON event_audit_logs(status);
CREATE INDEX idx_event_audit_logs_created_at ON event_audit_logs(created_at DESC);

