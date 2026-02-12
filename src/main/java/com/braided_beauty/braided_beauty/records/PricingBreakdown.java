package com.braided_beauty.braided_beauty.records;

import java.math.BigDecimal;

public record PricingBreakdown(
        BigDecimal subtotal,
        BigDecimal deposit,
        BigDecimal remainingBalance,   // baseline: subtotal - deposit
        BigDecimal discountAmount,     // computed from promo against remainingBalance
        BigDecimal remainingDue,       // remainingBalance - discountAmount (>= 0)
        BigDecimal tip,
        BigDecimal finalDue            // remainingDue + tip (>= 0)
) {
}
