package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleCalendarRepository extends JpaRepository<ScheduleCalendar, UUID> {
    Optional<ScheduleCalendar> findByNameIgnoreCase(String name);
    List<ScheduleCalendar> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sc FROM ScheduleCalendar sc WHERE sc.id = :id")
    Optional<ScheduleCalendar> findByIdForUpdate(@Param("id") UUID id);
}
