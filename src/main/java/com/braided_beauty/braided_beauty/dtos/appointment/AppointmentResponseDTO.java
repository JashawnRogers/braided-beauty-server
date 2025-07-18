package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AppointmentResponseDTO {
    @NotNull
    private final UUID id;
    @NotNull
    private final LocalDateTime appointmentTime;
    @NotNull
    private final AppointmentStatus appointmentStatus;
    @NotNull
    private final LocalDateTime createdAt;
    @NotNull
    private final ServiceResponseDTO service;
    private final Integer pointsEarned;
    private final LocalDateTime updatedAt;
}
