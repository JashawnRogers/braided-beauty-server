package com.braided_beauty.braided_beauty.dtos.shared;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class BusinessHoursRequestDTO {

    @NotNull
    private final DayOfWeek dayOfWeek;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final boolean isClosed;
}
