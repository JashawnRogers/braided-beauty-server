package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.records.ServicePopularityRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;

@Builder
@Getter
@AllArgsConstructor
public class AdminMonthlyAnalyticsDTO {
    private final YearMonth month;

    private final long completedAppointments;
    private final BigDecimal totalPaidByCard;
    private final BigDecimal totalPaidByCash;

    private final ServicePopularityRow mostPopularService;
    private final ServicePopularityRow leastPopularService;
}
