package com.braided_beauty.braided_beauty.dtos.timeSlots;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AvailableTimeSlotsDTO(String time, boolean available, LocalDateTime startTime, LocalDateTime endTime) {
}
