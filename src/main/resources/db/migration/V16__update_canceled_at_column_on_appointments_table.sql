ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS canceled_at_ts TIMESTAMP NULL;

ALTER TABLE appointments
    DROP COLUMN IF EXISTS canceled_at;

ALTER TABLE appointments
    RENAME COLUMN canceled_at_ts TO canceled_at;