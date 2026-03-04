package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.ScheduleCalendarHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleCalendarHoursRepository extends JpaRepository<ScheduleCalendarHours, UUID> {
    Optional<ScheduleCalendarHours> findByCalendar_IdAndDayOfWeek(UUID calendarId, DayOfWeek dayOfWeek);
    boolean existsByCalendar_IdAndDayOfWeek(UUID calendarId, DayOfWeek dayOfWeek);
    boolean existsByCalendar_IdAndDayOfWeekAndIdNot(UUID calendarId, DayOfWeek dayOfWeek, UUID id);
    List<ScheduleCalendarHours> findAllByCalendar_IdOrderByDayOfWeekAsc(UUID calendarId);
    void deleteAllByCalendar_Id(UUID calendarId);
}
