package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "business_hours")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BusinessHours {
    // Should be a single row in DB
    @Id
    @GeneratedValue
    private UUID id;

    private boolean isMondayOpen;
    private LocalTime mondayOpenTime;
    private LocalTime mondayCloseTime;

    private boolean isTuesdayOpen;
    private LocalTime tuesdayOpenTime;
    private LocalTime tuesdayCloseTime;

    private boolean isWednesdayOpen;
    private LocalTime wednesdayOpenTime;
    private LocalTime wednesdayCloseTime;

    private boolean isThursdayOpen;
    private LocalTime thursdayOpenTime;
    private LocalTime thursdayCloseTime;

    private boolean isFridayOpen;
    private LocalTime fridayOpenTime;
    private LocalTime fridayCloseTime;

    private boolean isSaturdayOpen;
    private LocalTime saturdayOpenTime;
    private LocalTime saturdayCloseTime;

    private boolean isSundayOpen;
    private LocalTime sundayOpenTime;
    private LocalTime sundayCloseTime;
}
