package com.braided_beauty.braided_beauty.dtos.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AdminCalendarDTO {
    private final UUID id;
    private final String name;
    private final String color;
    private final boolean active;
    private final Integer maxBookingsPerDay;
    private final LocalDateTime bookingOpenAt;
    private final LocalDateTime bookingCloseAt;
}
