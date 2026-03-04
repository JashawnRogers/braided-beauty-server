package com.braided_beauty.braided_beauty.dtos.calendar;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminCalendarUpdateRequestDTO {
    private String name;
    private String color;
    private Boolean active;
    private Integer maxBookingsPerDay;
    private LocalDateTime bookingOpenAt;
    private LocalDateTime bookingCloseAt;
}
