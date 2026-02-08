ALTER TABLE payment ADD COLUMN payment_method VARCHAR(20);
UPDATE payment SET payment_method = 'CARD' WHERE payment_method IS NULL;
ALTER TABLE payment ALTER COLUMN payment_method SET NOT NULL;