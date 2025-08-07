package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.PopularServiceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserAdminAnalyticsDTO {
    private final List<AppointmentResponseDTO> totalAppointmentsByMonth;
    private final int totalAppointmentsAllTime;
    private final List<UserSummaryResponseDTO> uniqueClientsThisMonth;
    private final PopularServiceDTO mostPopularService;
    private final int totalLoyaltyMembers;
}
