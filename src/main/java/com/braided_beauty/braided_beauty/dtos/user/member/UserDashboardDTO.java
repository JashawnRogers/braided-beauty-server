package com.braided_beauty.braided_beauty.dtos.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class UserDashboardDTO {
    private final UUID userId;
    private final String name;
    private final String email;
    private final LoyaltyRecord loyaltyRecord;
    private final Integer appointmentCount;
    private final AppointmentSummaryDTO nextApt;

}
