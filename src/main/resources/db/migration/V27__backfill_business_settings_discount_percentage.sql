UPDATE business_settings
SET discount_percentage = 0
WHERE discount_percentage IS NULL;

ALTER TABLE business_settings
    ALTER COLUMN discount_percentage SET DEFAULT 0,
    ALTER COLUMN discount_percentage SET NOT NULL;
