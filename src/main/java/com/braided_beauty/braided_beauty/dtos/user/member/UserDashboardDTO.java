package com.braided_beauty.braided_beauty.dtos.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.enums.LoyaltyTier;
import lombok.*;

import java.util.UUID;

@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class UserDashboardDTO {
    private final UUID userId;
    private final String name;
    private final String email;
    private final LoyaltyRecordResponseDTO loyaltyRecord;
    private final Integer appointmentCount;
    private final AppointmentSummaryDTO nextApt;
    private final LoyaltyTier loyaltyTier;
}
