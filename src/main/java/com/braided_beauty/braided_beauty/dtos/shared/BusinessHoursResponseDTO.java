package com.braided_beauty.braided_beauty.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class BusinessHoursResponseDTO {
   private final UUID id;
   private final DayOfWeek dayOfWeek;
   private final LocalTime openTime;
   private final LocalTime closeTime;
   private final boolean isClosed;
}
