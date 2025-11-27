package com.braided_beauty.braided_beauty.dtos.loyaltyRecord;

import lombok.*;

@Value
@Builder
@AllArgsConstructor
@Getter
@Setter
public class LoyaltyRecordResponseDTO {
    private final Integer points;
    private final Integer redeemedPoints;
    private final boolean isSignupBonusAwarded;
}
