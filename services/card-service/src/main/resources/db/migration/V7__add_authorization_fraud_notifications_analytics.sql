-- Authorization requests
CREATE TABLE IF NOT EXISTS authorization_requests (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant_name VARCHAR(255),
    merchant_category_code VARCHAR(10),
    merchant_country CHAR(2),
    authorization_status VARCHAR(32) NOT NULL,
    decline_reason VARCHAR(255),
    checked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_authorization_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_authorization_card_id ON authorization_requests (card_id);
CREATE INDEX IF NOT EXISTS idx_authorization_checked_at ON authorization_requests (checked_at);
CREATE INDEX IF NOT EXISTS idx_authorization_status ON authorization_requests (authorization_status);

-- Fraud detection events
CREATE TABLE IF NOT EXISTS fraud_events (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    description TEXT,
    transaction_id UUID,
    fraud_score NUMERIC(5, 2),
    detected_at TIMESTAMPTZ NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_fraud_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_fraud_card_id ON fraud_events (card_id);
CREATE INDEX IF NOT EXISTS idx_fraud_detected_at ON fraud_events (detected_at);
CREATE INDEX IF NOT EXISTS idx_fraud_resolved ON fraud_events (resolved);

-- Notifications
CREATE TABLE IF NOT EXISTS card_notifications (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    channel VARCHAR(16) NOT NULL, -- EMAIL, SMS, PUSH
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(16) NOT NULL, -- PENDING, SENT, FAILED
    sent_at TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_notifications_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_card_id ON card_notifications (card_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON card_notifications (notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON card_notifications (status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON card_notifications (created_at);

-- Card analytics summary (materialized view alternative - stored summary)
CREATE TABLE IF NOT EXISTS card_analytics (
    card_id UUID PRIMARY KEY,
    total_transactions INTEGER DEFAULT 0,
    total_amount NUMERIC(19, 4) DEFAULT 0,
    average_transaction_amount NUMERIC(19, 4) DEFAULT 0,
    declined_transactions INTEGER DEFAULT 0,
    last_transaction_date TIMESTAMPTZ,
    top_merchant_category VARCHAR(10),
    most_used_country CHAR(2),
    last_updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_analytics_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_analytics_last_updated ON card_analytics (last_updated_at);

-- Velocity tracking for fraud detection
CREATE TABLE IF NOT EXISTS velocity_tracking (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    window_type VARCHAR(16) NOT NULL, -- HOUR, DAY, WEEK
    window_start TIMESTAMPTZ NOT NULL,
    transaction_count INTEGER DEFAULT 0,
    total_amount NUMERIC(19, 4) DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_velocity_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_velocity_card_window ON velocity_tracking (card_id, window_type, window_start);

