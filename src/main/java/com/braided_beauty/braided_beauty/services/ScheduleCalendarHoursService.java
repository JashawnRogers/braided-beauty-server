package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.models.ScheduleCalendarHours;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleCalendarHoursService {
    private final ScheduleCalendarHoursRepository scheduleCalendarHoursRepository;

    public Optional<ScheduleCalendarHours> getByCalendarAndDay(UUID calendarId, DayOfWeek dayOfWeek) {
        return scheduleCalendarHoursRepository.findByCalendar_IdAndDayOfWeek(calendarId, dayOfWeek);
    }
}
