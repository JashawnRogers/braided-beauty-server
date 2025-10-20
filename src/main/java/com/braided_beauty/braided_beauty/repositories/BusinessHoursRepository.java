package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.UUID;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {
    boolean existsByDayOfWeek(DayOfWeek dayOfWeek);
    boolean existsByDayOfWeekAndIdNot(DayOfWeek dayOfWeek, UUID id);
}
