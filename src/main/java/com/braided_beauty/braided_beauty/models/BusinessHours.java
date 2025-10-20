package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "business_hours",
    uniqueConstraints = @UniqueConstraint(
            name = "uk_business_hours_day",
            columnNames = "day_of_week"
    )
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BusinessHours {
    // One row per day
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;


    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed;
}
