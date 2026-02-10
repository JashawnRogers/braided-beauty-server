ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS promo_code_id UUID NULL;

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS promo_code_text VARCHAR(64) NULL;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_promo_code
        FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id);

CREATE INDEX IF NOT EXISTS idx_appointments_promo_code_id ON appointments(promo_code_id);