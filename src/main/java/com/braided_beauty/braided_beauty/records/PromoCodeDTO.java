package com.braided_beauty.braided_beauty.records;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PromoCodeDTO(
        UUID id,
        String codeName,
        DiscountType discountType,
        BigDecimal value,
        Boolean active,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer maxRedemptions,
        Integer timesRedeemed
) {
}
