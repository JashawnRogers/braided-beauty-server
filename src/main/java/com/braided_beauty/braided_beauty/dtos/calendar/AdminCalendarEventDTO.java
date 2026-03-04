package com.braided_beauty.braided_beauty.dtos.calendar;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AdminCalendarEventDTO {
    private final UUID appointmentId;
    private final LocalDateTime appointmentTime;
    private final Integer durationMinutes;
    private final AppointmentStatus appointmentStatus;
    private final String serviceName;
    private final UUID calendarId;
    private final String calendarName;
    private final String calendarColor;
}
