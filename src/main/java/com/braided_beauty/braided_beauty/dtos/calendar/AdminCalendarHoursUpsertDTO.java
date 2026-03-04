package com.braided_beauty.braided_beauty.dtos.calendar;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class AdminCalendarHoursUpsertDTO {
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    private Boolean isClosed;
    private LocalTime openTime;
    private LocalTime closeTime;
}
