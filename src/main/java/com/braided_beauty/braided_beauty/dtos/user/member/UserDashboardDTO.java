package com.braided_beauty.braided_beauty.dtos.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import lombok.*;

import java.util.UUID;

@Value
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
}
