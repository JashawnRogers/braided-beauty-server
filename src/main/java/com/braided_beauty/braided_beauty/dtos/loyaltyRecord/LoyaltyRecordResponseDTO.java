package com.braided_beauty.braided_beauty.dtos.loyaltyRecord;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class LoyaltyRecordResponseDTO {
    private final Integer points;
    private final Integer redeemedPoints;
}
