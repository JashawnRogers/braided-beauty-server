package com.braided_beauty.braided_beauty.dtos.user.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class UserAdminAnalyticsDTO {
    private final int totalAppointmentsThisMonth;
    private final int totalAppointmentsAllTime;
    private final int uniqueClientsThisMonth;
    private final String mostPopularServiceName;
    private final int mostPopularServiceCount;
    private final BigDecimal totalRevenueThisMonth;
    private final BigDecimal totalRevenueAllTime;
    private final int activeLoyaltyMembers;
    private final double avgAppointmentsPerClient;
}
