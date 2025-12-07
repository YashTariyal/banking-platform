-- Account linking
ALTER TABLE cards ADD COLUMN IF NOT EXISTS account_id UUID;

-- CVV management
ALTER TABLE cards ADD COLUMN IF NOT EXISTS cvv_hash VARCHAR(255);
ALTER TABLE cards ADD COLUMN IF NOT EXISTS cvv_generated_at TIMESTAMPTZ;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS cvv_rotation_due_date TIMESTAMPTZ;

-- Cardholder name
ALTER TABLE cards ADD COLUMN IF NOT EXISTS cardholder_name VARCHAR(255);

-- ATM withdrawal limits
ALTER TABLE cards ADD COLUMN IF NOT EXISTS daily_atm_limit NUMERIC(19, 4);
ALTER TABLE cards ADD COLUMN IF NOT EXISTS monthly_atm_limit NUMERIC(19, 4);

-- Card renewal tracking
ALTER TABLE cards ADD COLUMN IF NOT EXISTS renewed_from_card_id UUID;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS renewal_count INTEGER DEFAULT 0;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS last_renewed_at TIMESTAMPTZ;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cards_account_id ON cards (account_id);
CREATE INDEX IF NOT EXISTS idx_cards_renewed_from ON cards (renewed_from_card_id);
CREATE INDEX IF NOT EXISTS idx_cards_cvv_rotation_due ON cards (cvv_rotation_due_date);

