package com.braided_beauty.braided_beauty.dtos.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class CancelAppointmentDTO {
    @NotNull(message = "Must provide appointment ID.")
    private final UUID appointmentId;
    @NotNull(message = "Must provide user ID.")
    private final UUID userId;
    @NotNull(message = "Must provide reason for cancellation.")
    private final String cancelReason;
}
