package com.braided_beauty.braided_beauty.records;

import java.math.BigDecimal;

public record BookingPricingPreview(
        BigDecimal subtotal,
        BigDecimal deposit,
        BigDecimal postDepositBalance,
        PromoPreviewResult promo
) {
}
