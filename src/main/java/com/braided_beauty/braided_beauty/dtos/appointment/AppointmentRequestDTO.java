package com.braided_beauty.braided_beauty.dtos.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AppointmentRequestDTO {
    @NotNull
    private final LocalDateTime appointmentTime;
    @NotNull
    private final UUID serviceId;
}
