package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.admin.AdminAllTimeAnalyticsDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.AdminMonthlyAnalyticsDTO;
import com.braided_beauty.braided_beauty.services.AdminAnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {
    private final AdminAnalyticsService adminAnalyticsService;


    /**
     * Example:
     * GET /api/v1/admin/analytics/monthly?month=2026-02
     *
     * If month is omitted, defaults to current month.
     */
    @GetMapping("/monthly")
    public ResponseEntity<AdminMonthlyAnalyticsDTO> getMonthlyAnalytics(
            @RequestParam(value = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month
    ) {
        YearMonth target = (month != null) ? month : YearMonth.now();
        return ResponseEntity.ok(adminAnalyticsService.getMonthlyAnalytics(target));
    }

    @GetMapping("/all-time")
    public ResponseEntity<AdminAllTimeAnalyticsDTO> getAllTimeAnalytics() {
        return ResponseEntity.ok(adminAnalyticsService.getAllTimeAnalytics());
    }
}
