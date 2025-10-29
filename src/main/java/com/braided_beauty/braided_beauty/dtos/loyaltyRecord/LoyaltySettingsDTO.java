package com.braided_beauty.braided_beauty.dtos.loyaltyRecord;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class LoyaltySettingsDTO {
    private UUID id;
    private Boolean programEnabled;
    private Integer signupBonusPoints;
    private Integer earnPerAppointment;
}
