-- Transaction history
CREATE TABLE IF NOT EXISTS card_transactions (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant_name VARCHAR(255),
    merchant_category_code VARCHAR(10),
    merchant_country CHAR(2),
    transaction_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    decline_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_card_transactions_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_card_transactions_card_id ON card_transactions (card_id);
CREATE INDEX IF NOT EXISTS idx_card_transactions_date ON card_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_card_transactions_type ON card_transactions (transaction_type);

-- Merchant category restrictions
CREATE TABLE IF NOT EXISTS card_merchant_restrictions (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    merchant_category_code VARCHAR(10) NOT NULL,
    action VARCHAR(16) NOT NULL, -- ALLOW or BLOCK
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_merchant_restrictions_card FOREIGN KEY (card_id) REFERENCES cards(id),
    CONSTRAINT uk_card_mcc UNIQUE (card_id, merchant_category_code)
);

CREATE INDEX IF NOT EXISTS idx_merchant_restrictions_card_id ON card_merchant_restrictions (card_id);

-- Geographic restrictions
CREATE TABLE IF NOT EXISTS card_geographic_restrictions (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    country_code CHAR(2) NOT NULL,
    action VARCHAR(16) NOT NULL, -- ALLOW or BLOCK
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_geographic_restrictions_card FOREIGN KEY (card_id) REFERENCES cards(id),
    CONSTRAINT uk_card_country UNIQUE (card_id, country_code)
);

CREATE INDEX IF NOT EXISTS idx_geographic_restrictions_card_id ON card_geographic_restrictions (card_id);

-- Contactless payment controls
ALTER TABLE cards ADD COLUMN IF NOT EXISTS contactless_enabled BOOLEAN DEFAULT TRUE;

-- Card-to-card transfers
CREATE TABLE IF NOT EXISTS card_transfers (
    id UUID PRIMARY KEY,
    from_card_id UUID NOT NULL,
    to_card_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    transfer_date TIMESTAMPTZ NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_transfers_from_card FOREIGN KEY (from_card_id) REFERENCES cards(id),
    CONSTRAINT fk_transfers_to_card FOREIGN KEY (to_card_id) REFERENCES cards(id)
);

CREATE INDEX IF NOT EXISTS idx_card_transfers_from ON card_transfers (from_card_id);
CREATE INDEX IF NOT EXISTS idx_card_transfers_to ON card_transfers (to_card_id);
CREATE INDEX IF NOT EXISTS idx_card_transfers_date ON card_transfers (transfer_date);

