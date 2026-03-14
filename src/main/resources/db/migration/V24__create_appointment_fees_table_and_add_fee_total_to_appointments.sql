ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS fee_total NUMERIC(10,2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS appointment_fees (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    fee_id UUID,
    fee_name VARCHAR(100) NOT NULL,
    fee_amount NUMERIC(10,2) NOT NULL CHECK (fee_amount >= 0),
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_appointment_fees_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_appointment_fees_fee
        FOREIGN KEY (fee_id)
        REFERENCES fees(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_appointment_fees_appointment_id
    ON appointment_fees(appointment_id);

CREATE INDEX IF NOT EXISTS idx_appointment_fees_fee_id
    ON appointment_fees(fee_id);