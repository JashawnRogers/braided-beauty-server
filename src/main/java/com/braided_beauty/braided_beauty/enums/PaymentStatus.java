package com.braided_beauty.braided_beauty.enums;

/**
 * Payment lifecycle for an appointment.
 * Notes:
 * - PENDING_PAYMENT: payment has been initiated (e.g., appointment created / checkout session created) but not yet confirmed by webhook.
 * - PAID_DEPOSIT: deposit payment succeeded.
 * - PAID_IN_FULL_ACH: paid-in-full (final payment succeeded).
 * - FAILED: a payment attempt failed.
 * - REFUNDED: refunded (full refund for the relevant payment scope).
 * - NO_DEPOSIT_REQUIRED: deposit amount was 0; appointment can proceed without deposit.
 */

public enum PaymentStatus {
    PENDING_PAYMENT,
    PAID_DEPOSIT,
    PAID_IN_FULL_ACH,
    PAID_IN_FULL_CASH,
    PAYMENT_FAILED,
    REFUNDED,
    NO_DEPOSIT_REQUIRED,
}
