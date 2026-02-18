-- Add pricing snapshot fields to appointments table
-- to prevent drift if service/add-on/promo values change later.

ALTER TABLE appointments
    ADD COLUMN service_price_at_booking NUMERIC(10, 2),
    ADD COLUMN add_ons_total_at_booking NUMERIC(10, 2),
    ADD COLUMN subtotal_at_booking NUMERIC(10, 2),
    ADD COLUMN post_deposit_balance_at_booking NUMERIC(10, 2);