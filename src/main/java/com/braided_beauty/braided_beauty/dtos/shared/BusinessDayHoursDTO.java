package com.braided_beauty.braided_beauty.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
public class BusinessDayHoursDTO {
    private final boolean isOpen;
    private final LocalDateTime openTime;
    private final LocalDateTime closeTime;
}
