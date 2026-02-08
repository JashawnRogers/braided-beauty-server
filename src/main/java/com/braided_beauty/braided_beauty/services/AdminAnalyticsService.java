package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.user.admin.AdminAllTimeAnalyticsDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.AdminMonthlyAnalyticsDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentMethod;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.records.ServicePopularityRow;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@AllArgsConstructor
public class AdminAnalyticsService {
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final PaymentRepository paymentRepository;

    public AdminMonthlyAnalyticsDTO getMonthlyAnalytics(YearMonth month) {
        DateRange range = monthToDateRange(month);

        long completedCount = appointmentRepository.countByAppointmentStatusAndCompletedAtBetween(
                AppointmentStatus.COMPLETED,
                range.start(),
                range.end()
        );

        List<PaymentStatus> paidStatuses = List.of(PaymentStatus.PAID_DEPOSIT, PaymentStatus.PAID_IN_FULL);

        BigDecimal cardTotal = paymentRepository.sumTotalForCompletedAppointmentsBetween(
                range.start(),
                range.end(),
                paidStatuses,
                PaymentMethod.CARD
        );

        BigDecimal cashTotal = paymentRepository.sumTotalForCompletedAppointmentsBetween(
                range.start(),
                range.end(),
                paidStatuses,
                PaymentMethod.CASH
        );

        ServicePopularityRow mostPopular = topMonthlyMostPopular(range);
        ServicePopularityRow leastPopular = topMonthlyLeastPopular(range);

        return AdminMonthlyAnalyticsDTO.builder()
                .month(month)
                .completedAppointments(completedCount)
                .totalPaidByCard(nz(cardTotal))
                .totalPaidByCash(nz(cashTotal))
                .mostPopularService(mostPopular)
                .leastPopularService(leastPopular)
                .build();
    }

    public AdminAllTimeAnalyticsDTO getAllTimeAnalytics() {
        long completedAllTime = appointmentRepository.countByAppointmentStatusAndCompletedAtIsNotNull(
                AppointmentStatus.COMPLETED
        );

        // All-time = from "forever" to now.
        // LocalDateTime.MIN can be annoying in some DBs, so use a safe old date.
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        List<PaymentStatus> paidStatuses = List.of(PaymentStatus.PAID_DEPOSIT, PaymentStatus.PAID_IN_FULL);

        BigDecimal cardAllTime = paymentRepository.sumTotalForCompletedAppointmentsBetween(
                start,
                end,
                paidStatuses,
                PaymentMethod.CARD
        );

        BigDecimal cashAllTime = paymentRepository.sumTotalForCompletedAppointmentsBetween(
                start,
                end,
                paidStatuses,
                PaymentMethod.CASH
        );

        ServicePopularityRow mostPopularAllTime = topAllTimeMostPopular();
        ServicePopularityRow leastPopularAllTime = topAllTimeLeastPopular();

        return AdminAllTimeAnalyticsDTO.builder()
                .completedAppointmentsAllTime(completedAllTime)
                .totalPaidByCardAllTime(nz(cardAllTime))
                .totalPaidByCashAllTime(nz(cashAllTime))
                .mostPopularServiceAllTime(mostPopularAllTime)
                .leastPopularServiceAllTime(leastPopularAllTime)
                .build();
    }

    // ---------- Helpers (private) ----------

    private ServicePopularityRow topMonthlyMostPopular(DateRange range) {
        return serviceRepository
                .findMostPopularServicesByCompletedBetween(
                        AppointmentStatus.COMPLETED,
                        range.start(),
                        range.end(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ServicePopularityRow topMonthlyLeastPopular(DateRange range) {
        return serviceRepository
                .findLeastPopularServicesByCompletedBetween(
                        AppointmentStatus.COMPLETED,
                        range.start(),
                        range.end(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ServicePopularityRow topAllTimeMostPopular() {
        return serviceRepository
                .findMostPopularServicesAllTime(
                        AppointmentStatus.COMPLETED,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ServicePopularityRow topAllTimeLeastPopular() {
        return serviceRepository
                .findLeastPopularServicesAllTime(
                        AppointmentStatus.COMPLETED,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Month-to-date rule:
     * - start = first day at 00:00
     * - end   = min(now, end of month at 23:59:59.999999999)
     */
    private DateRange monthToDateRange(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.isBefore(monthEnd) ? now : monthEnd;

        return new DateRange(start, end);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {}
}
