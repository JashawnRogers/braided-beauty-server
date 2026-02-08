-- 1) Normalize any legacy values just in case
UPDATE appointments
SET payment_status = 'PAID_IN_FULL'
WHERE payment_status IN ('PAID_IN_FULL_ACH', 'PAID_IN_FULL_CASH');

-- 2) Replace the check constraint
ALTER TABLE appointments
  DROP CONSTRAINT IF EXISTS appointments_payment_status_check;

ALTER TABLE appointments
  ADD CONSTRAINT appointments_payment_status_check
  CHECK (
    payment_status IN (
      'PENDING_PAYMENT',
      'PAID_DEPOSIT',
      'PAID_IN_FULL',
      'PAYMENT_FAILED',
      'REFUNDED',
      'NO_DEPOSIT_REQUIRED'
    )
  );