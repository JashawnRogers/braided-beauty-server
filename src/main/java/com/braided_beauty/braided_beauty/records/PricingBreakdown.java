package com.braided_beauty.braided_beauty.records;

import java.math.BigDecimal;

public record PricingBreakdown(
        BigDecimal subtotal,            // service + add-ons
        BigDecimal deposit,             // deposit required now (not discounted)
        BigDecimal postDepositBalance,  // subtotal - deposit
        BigDecimal promoDiscount,       // discount applied to postDepositBalance
        BigDecimal amountDueBeforeTip,  // postDepositBalance - promoDiscount (>= 0)
        BigDecimal tip,
        BigDecimal amountDue            // amountDueBeforeTip + tip (>= 0)
) {
}
