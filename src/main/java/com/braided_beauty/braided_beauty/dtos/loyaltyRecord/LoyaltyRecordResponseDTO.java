package com.braided_beauty.braided_beauty.dtos.loyaltyRecord;

import com.braided_beauty.braided_beauty.enums.LoyaltyTier;
import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class LoyaltyRecordResponseDTO {
    private final Integer points;
    private final Integer redeemedPoints;
    private final boolean isSignupBonusAwarded;
    private final LoyaltyTier loyaltyTier;
}
