package com.braided_beauty.braided_beauty.dtos.loyaltyRecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class LoyaltyRecordResponseDTO {
    private final Integer points;
    private final Integer redeemedPoints;
}
