CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    card_number VARCHAR(32) NOT NULL UNIQUE,
    masked_number VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    spending_limit NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cards_customer_id ON cards (customer_id);


