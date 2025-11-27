package com.braided_beauty.braided_beauty.mappers.loyaltyRecord;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltySettingsDTO;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.LoyaltySettings;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyRecordDtoMapper {

    public LoyaltyRecordResponseDTO toDTO(LoyaltyRecord loyaltyRecord){
        return LoyaltyRecordResponseDTO.builder()
                .points(loyaltyRecord.getPoints())
                .redeemedPoints(loyaltyRecord.getRedeemedPoints())
                .isSignupBonusAwarded(loyaltyRecord.isSignupBonusAwarded())
                .build();
    }

    public LoyaltySettingsDTO toDTO(LoyaltySettings settings) {
        return LoyaltySettingsDTO.builder()
                .id(settings.getId())
                .programEnabled(settings.isProgramEnabled())
                .earnPerAppointment(settings.getEarnPerAppointment())
                .signupBonusPoints(settings.getSignUpBonusPoints())
                .build();
    }

    public void toEntity(LoyaltySettingsDTO dto) {

    }
}
