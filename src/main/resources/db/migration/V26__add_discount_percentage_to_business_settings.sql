ALTER TABLE business_settings
ADD COLUMN discount_percentage NUMERIC(5,2) DEFAULT 0
CHECK (discount_percentage >= 0 AND discount_percentage <= 100);