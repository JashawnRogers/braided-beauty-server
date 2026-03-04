package com.braided_beauty.braided_beauty.dtos.calendar;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminCalendarCreateRequestDTO {
    @NotBlank
    private String name;
    private String color;
    private Boolean active;
    private Integer maxBookingsPerDay;
    private LocalDateTime bookingOpenAt;
    private LocalDateTime bookingCloseAt;
}
