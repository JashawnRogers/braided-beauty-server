package com.braided_beauty.braided_beauty.dtos.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AdminCalendarOverrideDTO {
    private final UUID id;
    private final LocalDate date;
    private final boolean isClosed;
    private final LocalTime openTime;
    private final LocalTime closeTime;
}
