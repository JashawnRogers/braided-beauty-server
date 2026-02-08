package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.records.ServicePopularityRow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
@AllArgsConstructor
public class AdminAllTimeAnalyticsDTO {
    private final long completedAppointmentsAllTime;
    private final BigDecimal totalPaidByCardAllTime;
    private final BigDecimal totalPaidByCashAllTime;

    private final ServicePopularityRow mostPopularServiceAllTime;
    private final ServicePopularityRow leastPopularServiceAllTime;
}
