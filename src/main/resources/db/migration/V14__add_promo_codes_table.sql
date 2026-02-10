CREATE TABLE IF NOT EXISTS promo_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    max_redemptions INTEGER NULL,
    times_redeemed INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE promo_codes
    ADD CONSTRAINT uk_promo_code UNIQUE (code);

CREATE INDEX IF NOT EXISTS idx_promo_codes_active ON promo_codes(active);