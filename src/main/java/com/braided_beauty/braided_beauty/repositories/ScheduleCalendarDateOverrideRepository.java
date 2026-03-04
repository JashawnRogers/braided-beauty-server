package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.ScheduleCalendarDateOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleCalendarDateOverrideRepository extends JpaRepository<ScheduleCalendarDateOverride, UUID> {
    Optional<ScheduleCalendarDateOverride> findByCalendar_IdAndDate(UUID calendarId, LocalDate date);
    List<ScheduleCalendarDateOverride> findAllByCalendar_IdAndDateBetweenOrderByDateAsc(UUID calendarId, LocalDate start, LocalDate end);
    void deleteAllByCalendar_Id(UUID calendarId);
}
