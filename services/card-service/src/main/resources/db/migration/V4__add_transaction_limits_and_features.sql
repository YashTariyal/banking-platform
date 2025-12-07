-- Daily and monthly transaction limits
ALTER TABLE cards ADD COLUMN IF NOT EXISTS daily_transaction_limit NUMERIC(19, 4);
ALTER TABLE cards ADD COLUMN IF NOT EXISTS monthly_transaction_limit NUMERIC(19, 4);

-- PIN management
ALTER TABLE cards ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255);
ALTER TABLE cards ADD COLUMN IF NOT EXISTS pin_attempts INTEGER DEFAULT 0;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMPTZ;

-- Card freeze/unfreeze
ALTER TABLE cards ADD COLUMN IF NOT EXISTS frozen BOOLEAN DEFAULT FALSE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMPTZ;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS frozen_reason VARCHAR(255);

-- Expiration date management
ALTER TABLE cards ADD COLUMN IF NOT EXISTS expiration_date DATE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS issued_at TIMESTAMPTZ;

-- Card replacement
ALTER TABLE cards ADD COLUMN IF NOT EXISTS replaced_by_card_id UUID;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS replacement_reason VARCHAR(255);
ALTER TABLE cards ADD COLUMN IF NOT EXISTS is_replacement BOOLEAN DEFAULT FALSE;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cards_replaced_by ON cards (replaced_by_card_id);
CREATE INDEX IF NOT EXISTS idx_cards_expiration_date ON cards (expiration_date);

