-- Add nullable fee columns for admin-added fees before final payment

ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS fee NUMERIC(10,2);

ALTER TABLE payment
  ADD COLUMN IF NOT EXISTS fee NUMERIC(10,2);