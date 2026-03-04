package com.braided_beauty.braided_beauty.dtos.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminCalendarHoursDTO {
    private final DayOfWeek dayOfWeek;
    private final boolean isClosed;
    private final LocalTime openTime;
    private final LocalTime closeTime;
}
