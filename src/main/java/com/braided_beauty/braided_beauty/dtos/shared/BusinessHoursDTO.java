package com.braided_beauty.braided_beauty.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class BusinessHoursDTO {
    private final BusinessDayHoursDTO monday;
    private final BusinessDayHoursDTO tuesday;
    private final BusinessDayHoursDTO wednesday;
    private final BusinessDayHoursDTO thursday;
    private final BusinessDayHoursDTO friday;
    private final BusinessDayHoursDTO saturday;
    private final BusinessDayHoursDTO sunday;
}
