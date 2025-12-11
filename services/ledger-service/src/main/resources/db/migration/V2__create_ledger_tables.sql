CREATE TABLE ledger_accounts
(
    id                  UUID PRIMARY KEY,
    name                VARCHAR(128)        NOT NULL,
    type                VARCHAR(32)         NOT NULL,
    status              VARCHAR(32)         NOT NULL,
    currency            VARCHAR(3)          NOT NULL,
    external_account_id VARCHAR(64),
    balance             NUMERIC(19, 4)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP           NOT NULL,
    updated_at          TIMESTAMP           NOT NULL,
    version             BIGINT              NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_ledger_accounts_external_id ON ledger_accounts (external_account_id) WHERE external_account_id IS NOT NULL;

CREATE TABLE ledger_journals
(
    id           UUID PRIMARY KEY,
    reference_id VARCHAR(64)  NOT NULL UNIQUE,
    description  VARCHAR(255),
    status       VARCHAR(32)  NOT NULL,
    posted_at    TIMESTAMP    NOT NULL,
    reversal_of  UUID,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE TABLE ledger_entries
(
    id                UUID PRIMARY KEY,
    journal_id        UUID           NOT NULL REFERENCES ledger_journals (id),
    ledger_account_id UUID           NOT NULL REFERENCES ledger_accounts (id),
    entry_type        VARCHAR(16)    NOT NULL,
    amount            NUMERIC(19, 4) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    description       VARCHAR(255),
    posted_at         TIMESTAMP      NOT NULL
);

CREATE INDEX idx_ledger_entries_account ON ledger_entries (ledger_account_id);
CREATE INDEX idx_ledger_entries_journal ON ledger_entries (journal_id);

