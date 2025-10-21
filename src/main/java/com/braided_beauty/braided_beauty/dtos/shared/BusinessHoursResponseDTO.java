package com.braided_beauty.braided_beauty.dtos.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class BusinessHoursResponseDTO {
   private final UUID id;
   private final String dayOfWeek;
   private final String openTime;
   private final String closeTime;
   private final String isClosed;
}
