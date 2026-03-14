CREATE TABLE IF NOT EXISTS fees (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fees_active ON fees(active);
CREATE INDEX IF NOT EXISTS idx_fees_name ON fees(name);