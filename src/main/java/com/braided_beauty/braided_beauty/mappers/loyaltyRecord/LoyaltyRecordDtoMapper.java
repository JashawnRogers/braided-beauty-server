package com.braided_beauty.braided_beauty.mappers.loyaltyRecord;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyRecordDtoMapper {

    public LoyaltyRecordResponseDTO toLoyaltyRecordResponseDTO(LoyaltyRecord loyaltyRecord){
        return LoyaltyRecordResponseDTO.builder()
                .points(loyaltyRecord.getPoints())
                .redeemedPoints(loyaltyRecord.getRedeemedPoints())
                .build();
    }
}
