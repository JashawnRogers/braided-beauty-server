ALTER TABLE payment
  DROP CONSTRAINT IF EXISTS payment_payment_status_check;

ALTER TABLE payment
  ADD CONSTRAINT payment_payment_status_check
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