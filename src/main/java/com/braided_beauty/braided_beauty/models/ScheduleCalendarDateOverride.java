package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "schedule_calendar_date_override",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_calendar_override_calendar_date",
                columnNames = {"calendar_id", "override_date"}
        )
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ScheduleCalendarDateOverride {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ScheduleCalendar calendar;

    @Column(name = "override_date", nullable = false)
    private LocalDate date;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;
}
