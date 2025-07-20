package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "business_hours")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BusinessHours {
    // Should be a single row in DB
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = true)
    private LocalTime openTime;

    @Column(nullable = true)
    private LocalTime closeTime;

    @Column(nullable = false)
    private boolean isClosed;
}
