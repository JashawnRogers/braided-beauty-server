package com.braided_beauty.braided_beauty.records;

import java.math.BigDecimal;
import java.util.UUID;

public record PromoPreviewResult(
        boolean valid,
        String message,
        UUID promoId,
        String promoLabel,
        BigDecimal discountAmount,
        BigDecimal remainingAfterPromo
) {
}
