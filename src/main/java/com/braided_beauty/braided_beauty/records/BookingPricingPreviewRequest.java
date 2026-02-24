package com.braided_beauty.braided_beauty.records;

import java.util.List;
import java.util.UUID;

public record BookingPricingPreviewRequest(
        UUID serviceId,
        List<UUID> addOnIds,
        String promoText
) {
}
